package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.jetbrains.python.debugger.PyDebugValue;
import com.jetbrains.python.debugger.PyDebuggerException;
import com.jetbrains.python.debugger.PyStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * The ObjectAnalyzer class is responsible for analyzing objects within Python stack frames.
 * It collects information about objects, such as their attributes and references to other objects,
 * and stores this information in a map for further use.
 *
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class ObjectAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(ObjectAnalyzer.class);

    private static final String ID_EXPRESSION_FORMAT = "id(%s)";
    private static final String DIR_EXPRESSION_FORMAT = "dir(%s)";
    private static final String UNKNOWN_VALUE = "unknown";
    private static final String PRIVATE_PREFIX = "__";
    private static final String PROTECTED_PREFIX = "_";
    private static final String STATIC_KEYWORD = "static";

    // Map to store objects, where the key is the object ID and the value is an array containing the name, type, current value, and visibility as ObjectInfo instances.
    private final Map<String, ObjectInfo> objects = new HashMap<>();

    // List of Python stack frames to be analyzed.
    private final List<PyStackFrame> pyStackFrames;

    /**
     * Constructor for ObjectAnalyzer.
     *
     * @param pyStackFrames a list of Python stack frames to analyze
     */
    public ObjectAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.pyStackFrames = pyStackFrames;
    }

    /**
     * Analyzes the objects in the stack frames.
     * This method iterates through the provided stack frames, collecting information about objects and their attributes.
     */
    public void analyzeObjects() {
        objects.clear();
        CountDownLatch latch = new CountDownLatch(this.pyStackFrames.size());

        for (PyStackFrame frame : this.pyStackFrames) {
            collectObjects(frame, latch);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Collects objects from a specific stack frame and stores them in the objects map.
     *
     * @param pyStackFrame the Python stack frame to analyze
     * @param latch a CountDownLatch to synchronize the completion of object collection
     */
    private void collectObjects(PyStackFrame pyStackFrame, CountDownLatch latch) {
        pyStackFrame.computeChildren(new XCompositeNode() {
            @Override
            public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                LOGGER.debug("Collecting objects for PyStackFrame: " + pyStackFrame.getFrameId());

                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    String id = determinePythonId(value, value.getName());

                    if (isUserDefinedInstance(value)) {
                        gatherAttributeInformation(value, id);
                    }
                }

                if (last) {
                    latch.countDown();
                    LOGGER.debug("Latch decremented, remaining count: " + latch.getCount());
                }
            }

            @Override
            public void tooManyChildren(int remaining) {}

            @Override
            public void tooManyChildren(int remaining, @NotNull Runnable addNextChildren) {
                XCompositeNode.super.tooManyChildren(remaining, addNextChildren);
            }

            @Override
            public void setAlreadySorted(boolean alreadySorted) {}

            @Override
            public void setErrorMessage(@NotNull String errorMessage) {}

            @Override
            public void setErrorMessage(@NotNull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {}

            @Override
            public void setMessage(@NotNull String message, @Nullable Icon icon, @NotNull SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {}
        });
    }

    /**
     * Gathers attribute information for a given object and stores it in the objects map.
     * It also handles references to other objects.
     *
     * @param value the Python debug value representing the object
     * @param pyObjId the ID of the Python object
     */
    private void gatherAttributeInformation(PyDebugValue value, String pyObjId) {
        // Evaluate dir() to get all attributes of the object
        String attributesListStr = evaluateExpression(value, String.format(DIR_EXPRESSION_FORMAT, value.getName()));
        String[] attributeNames = parseAttributeNames(attributesListStr);

        List<AttributeInfo> attributes = new ArrayList<>();
        List<String> references = new ArrayList<>();

        references.add(value.getName() + ":" + value.getType());

        for (String attrName : attributeNames) {
            attrName = attrName.trim().replace("'", ""); // Clean attribute name
            if (attrName.endsWith("__") || attrName.equals("_abc_impl")) {
                continue; // Skip attributes and methods ending with double underscore or that are from ABC module
            }

            PyDebugValue attrValue = evaluateExpressionValue(value, value.getName() + "." + attrName);
            if (attrValue == null || isMethod(Objects.requireNonNull(attrValue.getType()))) {
                continue;
            }

            String attrValueStr = determineAttributeValue(value, attrName, attrValue);
            String visibility = determineVisibility(value, attrName);

            attributes.add(new AttributeInfo(getOriginalAttributeName(value, attrName), attrValue.getType(), attrValueStr, visibility));
        }

        if (objects.containsKey(pyObjId)) {
            objects.get(pyObjId).references().add(value.getName() + ":" + value.getType());
        } else {
            objects.put(pyObjId, new ObjectInfo(references, attributes));
        }
    }

    /**
     * Parses the attribute names from the dir() output string.
     *
     * @param attributesListStr the output string from the dir() function
     * @return an array of attribute names
     */
    private String[] parseAttributeNames(String attributesListStr) {
        attributesListStr = attributesListStr.substring(1, attributesListStr.length() - 1); // Remove brackets
        return attributesListStr.split(", ");
    }

    /**
     * Determines the Python object's unique identifier (ID).
     * This method evaluates a Python expression to retrieve the object's ID using the built-in `id()` function.
     * If the ID cannot be determined due to the object overwriting built-in methods or attributes,
     * it attempts to retrieve the ID via `__builtins__.id()`.
     *
     * @param value the Python debug value representing the object
     * @param valueName the name of the Python object as a string
     * @return the ID of the Python object as a string, or an empty string if an error occurs
     */
    private String determinePythonId(PyDebugValue value, String valueName) {
        String id = evaluateExpression(value, String.format(ID_EXPRESSION_FORMAT, valueName));

        // In case some objects/variables are overwriting inbuilt attributes/methods
        if (id.contains("not callable")) {
            id = evaluateExpression(value, String.format("__builtins__.id(%s)", valueName));
        }

        return id;
    }

    /**
     * Determines the value of an attribute, including handling references to other objects.
     *
     * @param value the Python debug value representing the object
     * @param attrName the name of the attribute
     * @param attrValue the Python debug value representing the attribute's value
     * @return the attribute value as a string
     */
    private String determineAttributeValue(PyDebugValue value, String attrName, PyDebugValue attrValue) {
        if (Objects.requireNonNull(attrValue.getValue()).contains("object")) {
            // Get the ID of the referenced object
            try {
                String id = determinePythonId(value, value.getName() + "." + attrName);
                if (!Objects.requireNonNull(attrValue.getType()).contains("_abc_data")) {
                    gatherAttributeInformation(attrValue, id);
                }
                return "refid:" + id;
            } catch (Exception e) {
                LOGGER.error("Error getting ID for referenced object: " + attrName, e);
                return UNKNOWN_VALUE;
            }
        } else {
            return attrValue.getValue();
        }
    }

    /**
     * Determines if an attribute is a method.
     *
     * @param attrType the attribute type as a string
     * @return true if the type represents a method, false otherwise
     */
    private boolean isMethod(String attrType) {
        return attrType.equals("method") || attrType.equals("function") || attrType.equals("builtin_function_or_method") || attrType.startsWith("<bound method");
    }

    /**
     * Determines the visibility of an attribute or method based on its name.
     *
     * @param value the Python debug value representing the object
     * @param attributeName the name of the attribute or method
     * @return "private", "protected", or "public" based on the attribute's name
     */
    private String determineVisibility(PyDebugValue value, String attributeName) {
        if (attributeName.startsWith(PRIVATE_PREFIX) && !attributeName.endsWith(PRIVATE_PREFIX)) {
            return "private";
        } else if (attributeName.startsWith(PROTECTED_PREFIX)) {
            return "protected";
        } else if (isStaticAttribute(value, attributeName)) {
            return STATIC_KEYWORD;
        } else {
            return "public";
        }
    }

    /**
     * Determines if an attribute is static.
     *
     * @param value the Python debug value representing the object
     * @param attrName the name of the attribute
     * @return true if the attribute is static, false otherwise
     */
    private boolean isStaticAttribute(PyDebugValue value, String attrName) {
        try {
            String isClassVar = evaluateExpression(value, value.getName() + ".__class__.__dict__.get('" + attrName + "', None) is not None");
            return Boolean.parseBoolean(isClassVar);
        } catch (Exception e) {
            LOGGER.error("Error determining if attribute is static: " + attrName, e);
            return false;
        }
    }

    /**
     * Checks if the given value is a user-defined instance, excluding built-in types.
     *
     * @param value the Python debug value representing the object
     * @return true if the value is a user-defined instance, false otherwise
     */
    private boolean isUserDefinedInstance(PyDebugValue value) {
        String isInstance = evaluateExpression(value, "isinstance(" + value.getName() + ", object) and not isinstance(" + value.getName() + ", (int, float, str, bool, list, dict, tuple, set))");
        return Boolean.parseBoolean(isInstance);
    }

    /**
     * Evaluates a Python expression in the context of the stack frame and returns the result as a PyDebugValue.
     *
     * @param value the Python debug value representing the object
     * @param expression the Python expression to evaluate
     * @return the result of the expression as a PyDebugValue, or null if an error occurs
     */
    private PyDebugValue evaluateExpressionValue(PyDebugValue value, String expression) {
        try {
            return value.getFrameAccessor().evaluate(expression, false, true);
        } catch (PyDebuggerException e) {
            LOGGER.error("Error evaluating expression: " + expression, e);
            return null;
        }
    }

    /**
     * Evaluates a Python expression in the context of the stack frame.
     *
     * @param value the Python debug value representing the object
     * @param expression the Python expression to evaluate
     * @return the result of the expression as a string, or an empty string if an error occurs
     */
    private String evaluateExpression(PyDebugValue value, String expression) {
        try {
            return value.getFrameAccessor().evaluate(expression, false, true).getValue();
        } catch (PyDebuggerException e) {
            LOGGER.error("Error evaluating expression: " + expression, e);
            return "";
        }
    }

    /**
     * Restores the original attribute name for mangled private attributes.
     *
     * @param value the Python debug value representing the object
     * @param attributeName the mangled attribute name
     * @return the original attribute name, or the given attribute name if an error occurs
     */
    private String getOriginalAttributeName(PyDebugValue value, String attributeName) {
        try {
            String originalName = attributeName;
            if (attributeName.startsWith("_" + value.getType() + "__")) {
                // Restore the original attribute name for mangled private attributes
                originalName = attributeName.substring(attributeName.indexOf("__"));
            }
            return originalName;
        } catch (Exception e) {
            LOGGER.error("Error restoring original attribute name: " + attributeName, e);
            return attributeName;
        }
    }

    /**
     * Returns the collected objects map.
     *
     * @return a map where the key is the object ID and the value is an array containing the name, type, current value, and visibility
     */
    public Map<String, ObjectInfo> getObjects() {
        return objects;
    }
}
