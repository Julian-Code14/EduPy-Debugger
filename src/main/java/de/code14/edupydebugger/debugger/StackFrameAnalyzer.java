package de.code14.edupydebugger.debugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.frame.*;
import com.jetbrains.python.debugger.PyDebugValue;
import com.jetbrains.python.debugger.PyDebuggerException;
import com.jetbrains.python.debugger.PyStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author julian
 * @version 1.0
 * @since 17.07.24
 */
public class StackFrameAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(StackFrameAnalyzer.class);


    // Muster: Key: ID, Values -> [Name, Typ, aktueller Wert, Scope] ggf. zu erweitern um SourcePosition etc..
    private Map<String, List<String>> variables = new HashMap<>();

    // Muster: Key: ID, Values -> [Name, Typ, aktueller Wert, Sichtbarkeit] ggf. zu erweitern
    private Map<String, List<List<String>>> attributes = new HashMap<>();

    private List<PyStackFrame> pyStackFrames = new ArrayList<>();


    public StackFrameAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.pyStackFrames = pyStackFrames;
    }

    public void analyzeFrames() {
        variables.clear();
        CountDownLatch variableLatch = new CountDownLatch(this.pyStackFrames.size());

        LOGGER.debug("Anzahl der detektierten PyStackFrames: " + this.pyStackFrames.size());
        for (PyStackFrame frame : this.pyStackFrames) {
            collectVariables(frame, variableLatch);
        }

        try {
            variableLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        attributes.clear();
        CountDownLatch attributeLatch = new CountDownLatch(this.pyStackFrames.size());

        for (PyStackFrame frame : this.pyStackFrames) {
            collectAttributes(frame, attributeLatch);
        }

        try {
            attributeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private void collectVariables(PyStackFrame pyStackFrame, CountDownLatch latch) {
        pyStackFrame.computeChildren(new XCompositeNode() {
            @Override
            public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                LOGGER.debug("Analyzing PyStackFrame: " + pyStackFrame.getFrameId() + " und " + pyStackFrame.getThreadId() + " und " + pyStackFrame.getThreadFrameId());
                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    String id = "";
                    try {
                        id = value.getFrameAccessor().evaluate("__builtins__.id(" + value.getName() + ")", false, true).getValue();
                    } catch (PyDebuggerException e) {
                        throw new RuntimeException(e);
                    }
                    LOGGER.debug("collectAttributes: " + id + " -> " + value.getName() + ": " + value.getValue() + " (" + value.getType() + ") [" + determineScope(value) + "]");
                    variables.put(id, new ArrayList<>(Arrays.asList(value.getName(), value.getType(), value.getValue(), determineScope(value))));
                }
                if (last) {
                    latch.countDown();
                    LOGGER.debug("Latch decremented " + latch.getCount());
                }
            }

            @Override
            public void tooManyChildren(int remaining) {
            }

            @Override
            public void tooManyChildren(int remaining, @NotNull Runnable addNextChildren) {
                XCompositeNode.super.tooManyChildren(remaining, addNextChildren);
            }

            @Override
            public void setAlreadySorted(boolean alreadySorted) {
            }

            @Override
            public void setErrorMessage(@NotNull String errorMessage) {
            }

            @Override
            public void setErrorMessage(@NotNull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
            }

            @Override
            public void setMessage(@NotNull String message, @Nullable Icon icon, @NotNull SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {
            }

        });
    }

    private String determineScope(PyDebugValue value) {
        try {
            // Check if the variable exists in the local scope
            String isLocal = value.getFrameAccessor().evaluate("locals().get('" + value.getName() + "', None) is not None", false, true).getValue();
            if (Boolean.parseBoolean(isLocal)) {
                return "local";
            }

            // Check if the variable exists in the global scope
            String isGlobal = value.getFrameAccessor().evaluate("globals().get('" + value.getName() + "', None) is not None", false, true).getValue();
            if (Boolean.parseBoolean(isGlobal)) {
                return "global";
            }

            // If it's neither local nor global, it might be unknown or a different scope
            return "unknown";
        } catch (PyDebuggerException e) {
            LOGGER.error("Error determining scope for variable: " + value.getName(), e);
            return "unknown";
        }
    }

    private void collectAttributes(PyStackFrame pyStackFrame, CountDownLatch latch) {
        pyStackFrame.computeChildren(new XCompositeNode() {
            @Override
            public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                LOGGER.debug("Collecting attributes for PyStackFrame: " + pyStackFrame.getFrameId());
                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    String id = "";
                    try {
                        id = value.getFrameAccessor().evaluate("__builtins__.id(" + value.getName() + ")", false, true).getValue();
                    } catch (PyDebuggerException e) {
                        LOGGER.error("Error getting ID for variable: " + value.getName(), e);
                        continue;
                    }

                    // Check if this is a user-defined object instance
                    if (isUserDefinedInstance(value)) {
                        gatherAttributeInformation(value, id);
                    }
                }
                if (last) {
                    latch.countDown();
                    LOGGER.debug("Latch decremented " + latch.getCount());
                }
            }

            @Override
            public void tooManyChildren(int remaining) {
            }

            @Override
            public void tooManyChildren(int remaining, @NotNull Runnable addNextChildren) {
                XCompositeNode.super.tooManyChildren(remaining, addNextChildren);
            }

            @Override
            public void setAlreadySorted(boolean alreadySorted) {
            }

            @Override
            public void setErrorMessage(@NotNull String errorMessage) {
            }

            @Override
            public void setErrorMessage(@NotNull String errorMessage, @Nullable XDebuggerTreeNodeHyperlink link) {
            }

            @Override
            public void setMessage(@NotNull String message, @Nullable Icon icon, @NotNull SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {
            }

        });
    }

    private void gatherAttributeInformation(PyDebugValue value, String pyObjId) {
        try {
            // Evaluate dir() to get all attributes of the object
            String attributesListStr = value.getFrameAccessor().evaluate("dir(" + value.getName() + ")", false, true).getValue();
            attributesListStr = attributesListStr.substring(1, attributesListStr.length() - 1); // Remove brackets
            String[] attributeNames = attributesListStr.split(", ");

            List<List<String>> attributeList = new ArrayList<>();

            for (String attrName : attributeNames) {
                attrName = attrName.trim().replace("'", ""); // Clean attribute name
                if (attrName.endsWith("__")) {
                    continue; // Skip attributes and methods ending with double underscore
                }

                PyDebugValue attrValue = (PyDebugValue) value.getFrameAccessor().evaluate(value.getName() + "." + attrName, false, true);
                String attrType = attrValue.getType();

                // Exclude methods and only include attributes
                if (attrType.equals("method") || attrType.equals("builtin_function_or_method") || attrType.startsWith("<bound method")) {
                    continue;
                }

                // Determine if the attribute is a class variable (static) or an instance variable
                boolean isStatic = false;
                try {
                    String isClassVar = value.getFrameAccessor().evaluate(value.getName() + ".__class__.__dict__.get('" + attrName + "', None) is not None", false, true).getValue();
                    isStatic = Boolean.parseBoolean(isClassVar);
                } catch (PyDebuggerException e) {
                    LOGGER.error("Error determining if attribute is static: " + attrName, e);
                }

                // Determine the value or the ID of the referenced object
                String attrValueStr;
                if (Objects.requireNonNull(attrValue.getValue()).contains("object")) {
                    // Get the ID of the referenced object
                    try {
                        String id = value.getFrameAccessor().evaluate("__builtins__.id(" + value.getName() + "." + attrName + ")", false, true).getValue();
                        attrValueStr = "refid:" + id;
                        gatherAttributeInformation(attrValue, id);
                    } catch (PyDebuggerException e) {
                        LOGGER.error("Error getting ID for referenced object: " + attrName, e);
                        attrValueStr = "unknown";
                    }
                } else {
                    attrValueStr = attrValue.getValue();
                }

                String originalName = getOriginalAttributeName(value, attrName);
                String visibility = isStatic ? "static" : determineVisibility(originalName);
                attributeList.add(new ArrayList<>(Arrays.asList(originalName, attrType, attrValueStr, visibility)));
            }

            attributes.put(pyObjId, attributeList);
        } catch (PyDebuggerException e) {
            LOGGER.error("Error collecting attributes for object: " + value.getName(), e);
        }
    }

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

    private boolean isUserDefinedInstance(PyDebugValue value) {
        try {
            String isInstance = value.getFrameAccessor().evaluate("isinstance(" + value.getName() + ", object) and not isinstance(" + value.getName() + ", (int, float, str, bool, list, dict, tuple, set))", false, true).getValue();
            return Boolean.parseBoolean(isInstance);
        } catch (PyDebuggerException e) {
            LOGGER.error("Error checking if value is user-defined instance: " + value.getName(), e);
            return false;
        }
    }

    private String determineVisibility(String attributeName) {
        if (attributeName.startsWith("__") && !attributeName.endsWith("__")) {
            return "private";
        } else if (attributeName.startsWith("_")) {
            return "protected";
        } else {
            return "public";
        }
    }


    public Map<String, List<String>> getVariables() {
        return variables;
    }

    public Map<String, List<List<String>>> getAttributes() {
        return attributes;
    }

}
