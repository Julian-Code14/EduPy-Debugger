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
            System.out.println("Das Projektverzeichnis wurde nicht gefunden.");
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

                // Finde alle Attribute der Klasse
                List<PyTargetExpression> attributes = pyClass.getInstanceAttributes();
                for (PyTargetExpression attribute : attributes) {
                    String type = getTypeString(attribute, context);
                    attributesList.add(determineVisibility(Objects.requireNonNull(attribute.getName())) + attribute.getName() + " : " + type);
                }

                // Finde alle Methoden der Klasse
                List<PyFunction> methods = Arrays.asList(pyClass.getMethods());
                for (PyFunction method : methods) {
                    String methodSignature = getMethodSignature(method, context);
                    methodsList.add(methodSignature);
                }

                classDetails.put(className, new Object[]{attributesList, methodsList});
            }
        } else {
            LOGGER.warn("The psi file could not be found: " + virtualFile.getPath());
        }
    }

    private static String getMethodSignature(PyFunction method, TypeEvalContext context) {
        StringBuilder signature = new StringBuilder(determineVisibility(Objects.requireNonNull(method.getName())) + method.getName() + "(");
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
