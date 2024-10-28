package de.code14.edupydebugger.analysis.staticanalysis;

import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A utility class that provides helper methods for analyzing Python code elements.
 * This class includes methods for retrieving type information, determining visibility
 * of attributes and methods, and constructing method signatures.
 *
 * <p>The utility methods in this class are designed to work with the IntelliJ IDEA platform's
 * Python PSI (Program Structure Interface) and are intended to be used in conjunction with
 * code analysis tasks, such as generating class diagrams or performing static analysis.</p>
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *     <li>Determining the type of a Python element (attributes, parameters, functions).</li>
 *     <li>Determining the visibility of a Python class attribute or method based on its name.</li>
 *     <li>Constructing method signatures that include parameters and return types.</li>
 * </ul>
 *
 * @author julian
 * @version 0.2.0
 * @since 0.1.0
 */
public class PythonAnalysisHelper {

    // Set of default Python types that are not considered as references
    public static final Set<String> defaultTypes = new HashSet<>() {{
        add("int");
        add("float");
        add("str");
        add("bool");
        add("list");
        add("dict");
        add("tuple");
        add("set");
        add("None");
        add("?");
    }};

    /**
     * Retrieves the type of given Python element as a string.
     * This method is used for attributes, parameters, and functions.
     *
     * @param element the Python element to analyze
     * @param context the type evaluation context
     * @return the type of the element as a string, or "?" if the type could not be determined
     */
    public static String getTypeString(PyElement element, TypeEvalContext context) {
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
    public static String determineVisibility(String attributeName) {
        if (attributeName.startsWith("__") && !attributeName.endsWith("__")) {
            return "-";
        } else if (attributeName.startsWith("_")) {
            return "#";
        } else {
            return "+";
        }
    }

    /**
     * Constructs the method signature for a given Python method, including its parameters and return type.
     *
     * @param method the Python method to analyze
     * @param context the type evaluation context
     * @return a string representing the method signature
     */
    public static String getMethodSignature(PyFunction method, TypeEvalContext context) {
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
            if (paramType.startsWith("{")) {
                paramType = "?";
            }
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
}
