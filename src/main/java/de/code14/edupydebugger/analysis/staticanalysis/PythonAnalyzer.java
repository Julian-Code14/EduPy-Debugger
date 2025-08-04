package de.code14.edupydebugger.analysis.staticanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.TypeEvalContext;

import java.util.*;

/**
 * A utility class for analyzing Python files within a project.
 * The class extracts class details such as attributes, methods, references, and superclasses
 * from Python files and stores them for further processing.
 * <p>
 * The analysis includes both static code analysis and gathering type information using the PyCharm API.
 */
public class PythonAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(PythonAnalyzer.class);

    // Holds details about classes, with the class name as the key and an array of class information as the value.
    private final Map<String, ClassInfo> classDetails = new HashMap<>();

    /**
     * Analyzes all Python files in the given project by traversing the project directory recursively.
     * Extracts information about Python classes found within the files.
     *
     * @param project the project to be analyzed
     */
    public void analyzePythonFiles(Project project) {
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

        // Iteratively traverse the project directory and analyze files
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
    private void analyzeDirectory(Project project, VirtualFile rootDir, String projectBasePath) {
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
                    if (file.getFileType() == PythonFileType.INSTANCE && isPythonFileInProject(file, projectBasePath)) {
                        analyzePythonClassFile(project, file);
                    }
                }
            }
        }
    }

    /**
     * Determines whether the given file is a user-defined Python file based on its path.
     *
     * @param file the file to check
     * @param projectBasePath the base path of the project
     * @return true if the file is a user-defined Python file, false otherwise
     */
    private static boolean isPythonFileInProject(VirtualFile file, String projectBasePath) {
        return file.getFileType() == PythonFileType.INSTANCE && file.getPath().startsWith(projectBasePath);
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
    private void analyzePythonClassFile(Project project, VirtualFile virtualFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        if (psiFile == null || psiFile.getFileType() != PythonFileType.INSTANCE) {
            LOGGER.warn("The psi file could not be found: " + virtualFile.getPath());
            return;
        }

        if (psiFile instanceof PyFile pyFile) {
            TypeEvalContext context = TypeEvalContext.codeAnalysis(project, psiFile);
            processPyFile(pyFile, context);
        } else {
            LOGGER.warn("The psi file could not be found: " + virtualFile.getPath());
        }
    }

    /**
     * Processes a PyFile and collects class details such as attributes, methods, references, and superclasses.
     *
     * @param pyFile the Python file to process
     * @param context the type evaluation context
     */
    private void processPyFile(PyFile pyFile, TypeEvalContext context) {
        for (PyClass pyClass : pyFile.getTopLevelClasses()) {
            String className = pyClass.getName();
            ClassInfo classInfo = new ClassInfo(
                    collectAttributes(pyClass, context),
                    collectMethods(pyClass, context),
                    collectReferences(pyClass, context),
                    collectSuperClasses(pyClass)
            );
            classDetails.put(className, classInfo);
        }
    }

    /**
     * Collects all attributes of a Python class.
     *
     * @param pyClass the Python class to analyze
     * @param context the type evaluation context
     * @return a list of attribute descriptions
     */
    private List<String> collectAttributes(PyClass pyClass, TypeEvalContext context) {
        List<String> attributesList = new ArrayList<>();
        List<PyTargetExpression> instanceAttributes = pyClass.getInstanceAttributes();
        List<PyTargetExpression> classAttributes = pyClass.getClassAttributes();
        List<PyTargetExpression> attributes = new ArrayList<>(instanceAttributes);
        attributes.addAll(classAttributes);

        for (PyTargetExpression attribute : attributes) {
            String staticModifier = pyClass.findClassAttribute(Objects.requireNonNull(attribute.getName()), false, null) != null ? "{static} " : "";
            String visibility = PythonAnalysisHelper.determineVisibility(attribute.getName());
            String type = PythonAnalysisHelper.getTypeString(attribute, context);
            attributesList.add(staticModifier + visibility + attribute.getName() + " : " + type);
        }

        return attributesList;
    }

    /**
     * Collects all methods of a Python class.
     *
     * @param pyClass the Python class to analyze
     * @param context the type evaluation context
     * @return a list of method signatures
     */
    private List<String> collectMethods(PyClass pyClass, TypeEvalContext context) {
        List<String> methodsList = new ArrayList<>();
        for (PyFunction method : pyClass.getMethods()) {
            methodsList.add(PythonAnalysisHelper.getMethodSignature(method, context));
        }
        return methodsList;
    }

    /**
     * Collects all references (types that are not primitive types) of a Python class.
     *
     * @param pyClass the Python class to analyze
     * @param context the type evaluation context
     * @return a list of reference types
     */
    private List<String> collectReferences(PyClass pyClass, TypeEvalContext context) {
        Set<String> referencesSet = new HashSet<>();
        for (String attribute : collectAttributes(pyClass, context)) {
            String type = attribute.split(" : ")[1];
            if (!PythonAnalysisHelper.defaultTypes.contains(type)) {
                referencesSet.add(type);
            }
        }
        return new ArrayList<>(referencesSet);
    }

    /**
     * Collects all superclasses of a Python class.
     *
     * @param pyClass the Python class to analyze
     * @return a list of superclasses
     */
    private List<String> collectSuperClasses(PyClass pyClass) {
        List<String> superClassesList = new ArrayList<>();
        for (PyExpression superClass : pyClass.getSuperClassExpressions()) {
            superClassesList.add(superClass.getText());
        }
        return superClassesList;
    }


    /**
     * Returns the collected details about the analyzed classes.
     *
     * @return a map where the key is the class name and the value is an array of details (attributes, methods, references, superclasses)
     */
    public Map<String, ClassInfo> getClassDetails() {
        return classDetails;
    }

}
