package de.code14.edupydebugger.debugger;

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
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author julian
 * @version 1.0
 * @since 16.07.24
 */
public class PythonAnalyzer {

    private final static Logger LOGGER = Logger.getInstance(PythonAnalyzer.class);

    private static Map<String, Object[]> classDetails = new HashMap<>();
    private static final Set<String> defaultTypes = new HashSet<String>() {
        {add("int");add("float");add("str");add("bool");add("list");add("dict");add("tuple");add("set");}
    };

    public static void analyzePythonFile(Project project) {
        // Erhalte den BasePath des Projekts
        String projectBasePath = project.getBasePath();

        if (projectBasePath == null) {
            LOGGER.warn("The project has no base path.");
            return;
        }

        // Lade das Projektverzeichnis
        VirtualFile projectDir = LocalFileSystem.getInstance().findFileByPath(projectBasePath);

        if (projectDir == null) {
            LOGGER.warn("The project dir could not be found.");
            return;
        }

        // Rekursiv durch alle Dateien im Projektverzeichnis iterieren und analysieren
        analyzeDirectory(project, projectDir, projectBasePath);
    }

    private static void analyzeDirectory(Project project, VirtualFile directory, String projectBasePath) {
        for (VirtualFile file : directory.getChildren()) {
            if (file.isDirectory()) {
                // Bibliotheksverzeichnisse ausschließen
                if (!isLibraryDirectory(file)) {
                    // Rekursiv die Unterverzeichnisse analysieren
                    analyzeDirectory(project, file, projectBasePath);
                }
            } else {
                // Nur Python-Dateien analysieren, die innerhalb des Projektverzeichnisses liegen
                if (file.getFileType() == PythonFileType.INSTANCE && isUserFile(file, projectBasePath)) {
                    analyzePythonFile(project, file);
                }
            }
        }
    }

    private static boolean isUserFile(VirtualFile file, String projectBasePath) {
        // Überprüfen, ob der Dateipfad mit dem Projekt-BasePath beginnt
        return file.getPath().startsWith(projectBasePath);
    }

    private static boolean isLibraryDirectory(VirtualFile directory) {
        String[] libraryDirs = {"site-packages", "dist-packages", "lib", "libs", "Library", "Frameworks"};
        for (String libraryDir : libraryDirs) {
            if (directory.getPath().contains(libraryDir)) {
                return true;
            }
        }
        return false;
    }

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

                // Finde alle Attribute der Klasse
                List<PyTargetExpression> attributes = pyClass.getInstanceAttributes();
                attributes.addAll(pyClass.getClassAttributes());
                for (PyTargetExpression attribute : attributes) {
                    String staticModifier = pyClass.findClassAttribute(Objects.requireNonNull(attribute.getName()), false, null) != null ? "{static} " : "";
                    String visibility = determineVisibility(Objects.requireNonNull(attribute.getName()));
                    String type = getTypeString(attribute, context);
                    attributesList.add(staticModifier + visibility + attribute.getName() + " : " + type);

                    // Is it a reference?
                    if (!defaultTypes.contains(type)) {
                        referencesList.add(type);
                    }
                }

                // Finde alle Methoden der Klasse
                List<PyFunction> methods = Arrays.asList(pyClass.getMethods());
                for (PyFunction method : methods) {
                    String methodSignature = getMethodSignature(method, context);
                    methodsList.add(methodSignature);
                }

                // Finde alle Superklassen der Klasse
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

        // Return types
        String returnType = getTypeString(method, context);
        if (!returnType.equals("?")) {
            signature.append(" : ").append(returnType);
        }
        return signature.toString();
    }


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

    private static String determineVisibility(String attributeName) {
        if (attributeName.startsWith("__") && !attributeName.endsWith("__")) {
            return "-";
        } else if (attributeName.startsWith("_")) {
            return "#";
        } else {
            return "+";
        }
    }

    public static Map<String, Object[]> getClassDetails() {
        return classDetails;
    }

}
