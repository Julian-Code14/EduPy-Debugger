package de.code14.edupydebugger.analysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;

import java.util.*;

/**
 * A utility class for analyzing Python files within a project.
 * The class extracts class details such as attributes, methods, references, and superclasses
 * from Python files and stores them for further processing.
 * <p>
 * The analysis includes both static code analysis and gathering type information using the PyCharm API.
 *
 * @author julian
 * @version 1.0
 * @since 16.07.24
 */
public class PythonAnalyzer {

    private final static Logger LOGGER = Logger.getInstance(PythonAnalyzer.class);

    // Holds details about classes, with the class name as the key and an array of class information as the value.
    private static final Map<String, Object[]> classDetails = new HashMap<>();

    // Set of default Python types that are not considered as references
    private static final Set<String> defaultTypes = new HashSet<>() {{
        add("int");
        add("float");
        add("str");
        add("bool");
        add("list");
        add("dict");
        add("tuple");
        add("set");
    }};

    /**
     * Analyzes all Python files in the given project by traversing the project directory recursively.
     * Extracts information about Python classes found within the files.
     *
     * @param project the project to be analyzed
     */
    public static void analyzePythonFile(Project project) {
        // Get the base path of the project
        String projectBasePath = project.getBasePath();

        if (projectBasePath == null) {
            LOGGER.warn("The project has no base path.");
            return;
        }

        // Load the project directory
        VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(projectBasePath);

        if (projectDir == null) {
            LOGGER.warn("The project directory could not be found.");
            return;
        }

        // Recursively iterate through all files in the project directory and analyze them
        analyzeDirectory(project, projectDir, projectBasePath);
    }

    /**
     * Iteratively analyzes directories and their contained files.
     * Excludes library directories and only analyzes user-defined Python files.
     *
     * @param project the project context
     * @param rootDir the root directory to start analyzing
     * @param projectBasePath the base path of the project
     */
    private static void analyzeDirectory(Project project, VirtualFile rootDir, String projectBasePath) {
        Stack<VirtualFile> dirsToAnalyze = new Stack<>();
        dirsToAnalyze.push(rootDir);

        while (!dirsToAnalyze.isEmpty()) {
            VirtualFile directory = dirsToAnalyze.pop();

            for (VirtualFile file : directory.getChildren()) {
                if (file.isDirectory()) {
                    // Exclude library directories
                    if (!isLibraryDirectory(file)) {
                        dirsToAnalyze.push(file);
                    }
                } else {
                    // Analyze only Python files that are within the project directory
                    if (file.getFileType() == PythonFileType.INSTANCE && isUserFile(file, projectBasePath)) {
                        analyzePythonFile(project, file);
                    }
                }
            }
        }
    }

    /**
     * Determines whether the given file is a user-defined file based on its path.
     *
     * @param file the file to check
     * @param projectBasePath the base path of the project
     * @return true if the file is a user-defined file, false otherwise
     */
    private static boolean isUserFile(VirtualFile file, String projectBasePath) {
        return file.getPath().startsWith(projectBasePath);
    }

    /**
     * Determines whether the given directory is a library directory.
     * Library directories are excluded from the analysis.
     *
     * @param directory the directory to check
     * @return true if the directory is a library directory, false otherwise
     */
    private static boolean isLibraryDirectory(VirtualFile directory) {
        String[] libraryDirs = {"site-packages", "dist-packages", "lib", "libs", "Library", "Frameworks"};
        for (String libraryDir : libraryDirs) {
            if (directory.getPath().contains(libraryDir)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Analyzes a specific Python file, extracting class information such as attributes, methods, references, and superclasses.
     *
     * @param project the project context
     * @param virtualFile the Python file to analyze
     */
    private static void analyzePythonFile(Project project, VirtualFile virtualFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (psiFile == null || psiFile.getFileType() != PythonFileType.INSTANCE) {
            LOGGER.warn("The psi file could not be found: " + virtualFile.getPath());
            return;
        }

        if (psiFile instanceof PyFile pyFile) {
            TypeEvalContext context = TypeEvalContext.codeAnalysis(project, psiFile);
            List<PyClass> pyClasses = pyFile.getTopLevelClasses();

            for (PyClass pyClass : pyClasses) {
                String className = pyClass.getName();
                List<String> attributesList = new ArrayList<>();
                List<String> methodsList = new ArrayList<>();
                List<String> referencesList = new ArrayList<>();
                List<String> superClassesList = new ArrayList<>();

                // Collect all attributes of the class
                List<PyTargetExpression> instanceAttributes = pyClass.getInstanceAttributes();
                List<PyTargetExpression> classAttributes = pyClass.getClassAttributes();
                Set<PyTargetExpression> attributes = new HashSet<>();
                attributes.addAll(instanceAttributes);
                attributes.addAll(classAttributes);

                for (PyTargetExpression attribute : attributes) {
                    String staticModifier = pyClass.findClassAttribute(Objects.requireNonNull(attribute.getName()), false, null) != null ? "{static} " : "";
                    String visibility = determineVisibility(Objects.requireNonNull(attribute.getName()));
                    String type = getTypeString(attribute, context);
                    attributesList.add(staticModifier + visibility + attribute.getName() + " : " + type);

                    // Check if the attribute type is a reference to another class
                    if (!defaultTypes.contains(type)) {
                        referencesList.add(type);
                    }
                }

                // Collect all methods of the class
                PyFunction[] methods = pyClass.getMethods();
                for (PyFunction method : methods) {
                    String methodSignature = getMethodSignature(method, context);
                    methodsList.add(methodSignature);
                }

                // Collect all superclasses of the class
                List<PyExpression> superClasses = List.of(pyClass.getSuperClassExpressions());
                for (PyExpression superClass : superClasses) {
                    superClassesList.add(superClass.getText());
                }

                classDetails.put(className, new Object[]{attributesList, methodsList, referencesList, superClassesList});
            }
        } else {
            LOGGER.warn("The psi file could not be found: " + virtualFile.getPath());
        }
    }

    /**
     * Constructs the method signature for a given Python method, including its parameters and return type.
     *
     * @param method the Python method to analyze
     * @param context the type evaluation context
     * @return a string representing the method signature
     */
    private static String getMethodSignature(PyFunction method, TypeEvalContext context) {
        StringBuilder signature = new StringBuilder();
        // Check if the method is static
        if (method.getDecoratorList() != null && method.getDecoratorList().findDecorator("staticmethod") != null) {
            signature.append("{static} ");
        }
        // Check if the method is abstract
        if (method.getDecoratorList() != null && method.getDecoratorList().findDecorator("abstractmethod") != null) {
            signature.append("{abstract} ");
        }

        signature.append(determineVisibility(Objects.requireNonNull(method.getName()))).append(method.getName()).append("(");
        PyParameterList parameterList = method.getParameterList();
        PyParameter[] parameters = parameterList.getParameters();

        // Append method parameters to the signature
        for (int i = 0; i < parameters.length; i++) {
            PyParameter parameter = parameters[i];
            String paramName = parameter.getName();
            String paramType = getTypeString(parameter, context);
            if (paramType.equals("?")) {
                signature.append(paramName);
            } else {
                signature.append(paramName).append(" : ").append(paramType);
            }
            if (i < parameters.length - 1) {
                signature.append(", ");
            }
        }

        signature.append(")");

        // Add return type to the method signature
        String returnType = getTypeString(method, context);
        if (!returnType.equals("?")) {
            signature.append(" : ").append(returnType);
        }
        return signature.toString();
    }


    /**
     * Retrieves the type of a given Python element as a string.
     * This method is used for attributes, parameters, and functions.
     *
     * @param element the Python element to analyze
     * @param context the type evaluation context
     * @return the type of the element as a string, or "?" if the type could not be determined
     */
    private static String getTypeString(PyElement element, TypeEvalContext context) {
        if (element instanceof PyTargetExpression) {
            PyType type = context.getType((PyTargetExpression) element);
            return type != null ? type.getName() : "?";
        } else if (element instanceof PyNamedParameter) {
            PyType type = context.getType((PyNamedParameter) element);
            return type != null ? type.getName() : "?";
        } else if (element instanceof PyFunction) {
            PyType type = context.getReturnType((PyFunction) element);
            return type != null ? type.getName() : "?";
        } else {
            return "?";
        }
    }

    /**
     * Determines the visibility of a Python class attribute or method based on its name.
     *
     * @param attributeName the name of the attribute or method
     * @return "+" for public, "#" for protected, "-" for private
     */
    private static String determineVisibility(String attributeName) {
        if (attributeName.startsWith("__") && !attributeName.endsWith("__")) {
            return "-";
        } else if (attributeName.startsWith("_")) {
            return "#";
        } else {
            return "+";
        }
    }

    /**
     * Returns the collected details about the analyzed classes.
     *
     * @return a map where the key is the class name and the value is an array of details (attributes, methods, references, superclasses)
     */
    public static Map<String, Object[]> getClassDetails() {
        return classDetails;
    }

}
