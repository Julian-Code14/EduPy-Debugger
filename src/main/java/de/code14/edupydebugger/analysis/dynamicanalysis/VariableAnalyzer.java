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
 * The VariableAnalyzer class is responsible for analyzing variables within Python stack frames.
 */
public class VariableAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(VariableAnalyzer.class);

    private static final String ID_EXPRESSION_FORMAT = "__builtins__.id(%s)";

    // Map to store variables, where the key is the variable ID and the value is a list containing the name, type, current value, and scope.
    private final Map<String, List<String>> variables = new HashMap<>();

    // List of Python stack frames to be analyzed.
    private final List<PyStackFrame> pyStackFrames;

    /**
     * Constructor for VariableAnalyzer.
     *
     * @param pyStackFrames a list of Python stack frames to analyze
     */
    public VariableAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.pyStackFrames = pyStackFrames;
    }

    /**
     * Analyzes the variables in the stack frames.
     */
    public void analyzeVariables() {
        variables.clear();
        CountDownLatch latch = new CountDownLatch(this.pyStackFrames.size());

        for (PyStackFrame frame : this.pyStackFrames) {
            collectVariables(frame, latch);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Collects variables from a specific stack frame and stores them in the variables map.
     *
     * @param pyStackFrame the Python stack frame to analyze
     * @param latch a CountDownLatch to synchronize the completion of variable collection
     */
    private void collectVariables(PyStackFrame pyStackFrame, CountDownLatch latch) {
        pyStackFrame.computeChildren(new XCompositeNode() {
            @Override
            public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                LOGGER.debug("Analyzing PyStackFrame: " + pyStackFrame.getFrameId());

                // Keep a small set of names visible directly in this frame
                Set<String> namesInFrame = new HashSet<>();
                PyDebugValue evalCtx = null;
                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    if (evalCtx == null) evalCtx = value; // use first available value as evaluation context
                    String id = determinePythonId(value, value.getName());
                    // If the file changes, variables from another file would not be defined -> exclude
                    if (!id.contains("is not defined")) {
                        namesInFrame.add(value.getName());
                        if (variables.containsKey(id)) { // If there are more names for an id
                            variables.get(id).set(0, variables.get(id).get(0) + "###" + value.getName());
                        } else { // Default: new variable found -> put key-value-pair into the map
                            variables.put(id, new ArrayList<>(Arrays.asList(
                                    value.getName(),
                                    value.getType(),
                                    Objects.requireNonNull(value.getValue()).replace(", ", "~"),
                                    determineScope(value)
                            )));
                        }
                    }
                }

                // Additionally enrich with globals (even when stopped in a local scope)
                // Best-effort: only when we have an evaluation context
                if (evalCtx != null) {
                    try {
                        List<String> globalNames = parsePythonList(evaluateExpression(evalCtx, "list(globals().keys())"));
                        for (String rawName : globalNames) {
                            String name = rawName.replace("'", "").trim();
                            if (name.isEmpty()) continue;

                            // Evaluate the global value itself for type and repr
                            PyDebugValue gv = evaluateExpressionValue(evalCtx, "globals().get('" + name + "', None)");
                            if (gv == null || gv.getValue() == null) continue;

                            // Skip noisy entries that are not user variables
                            String t = gv.getType();
                            if (t == null) continue;
                            if (t.equals("module") || t.equals("function") || t.equals("builtin_function_or_method") || t.equals("type")) {
                                continue;
                            }

                            String id = determinePythonId(evalCtx, "globals()['" + name + "']");
                            if (id.contains("is not defined")) continue;

                            // If this object id is already known, just merge the name; else add as global variable
                            if (variables.containsKey(id)) {
                                List<String> meta = variables.get(id);
                                if (meta != null) {
                                    String existing = meta.get(0);
                                    // Avoid duplicate name merges
                                    if (!Arrays.asList(existing.split("###")).contains(name)) {
                                        meta.set(0, existing + "###" + name);
                                    }
                                }
                            } else {
                                variables.put(id, new ArrayList<>(Arrays.asList(
                                        name,
                                        t,
                                        Objects.requireNonNull(gv.getValue()).replace(", ", "~"),
                                        "global"
                                )));
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.debug("Global enrichment failed (non-fatal)", ex);
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
     * Determines the scope (local, global, or unknown) of a variable within the stack frame.
     *
     * @param value the Python debug value representing the variable
     * @return the scope of the variable as a string
     */
    private String determineScope(PyDebugValue value) {
        String isLocal = evaluateExpression(value, "locals().get('" + value.getName() + "', None) is not None");
        if (Boolean.parseBoolean(isLocal)) {
            return "local";
        }

        String isGlobal = evaluateExpression(value, "globals().get('" + value.getName() + "', None) is not None");
        if (Boolean.parseBoolean(isGlobal)) {
            return "global";
        }

        return "unknown";
    }

    /**
     * Evaluates a Python expression in the context of the stack frame.
     *
     * @param value the Python debug value
     * @param expression the Python expression to evaluate
     * @return the result of the expression as a string
     */
    private String evaluateExpression(PyDebugValue value, String expression) {
        try {
            return value.getFrameAccessor().evaluate(expression, false, true).getValue();
        } catch (PyDebuggerException e) {
            LOGGER.warn("Error evaluating expression: " + expression, e);
            return "";
        }
    }

    /**
     * Evaluates a Python expression in the current frame and returns the raw {@link PyDebugValue}.
     * Returns {@code null} on failure.
     */
    private @Nullable PyDebugValue evaluateExpressionValue(PyDebugValue value, String expression) {
        try {
            return value.getFrameAccessor().evaluate(expression, false, true);
        } catch (PyDebuggerException e) {
            LOGGER.warn("Error evaluating expression: " + expression, e);
            return null;
        }
    }

    /**
     * Parses a Python list string representation like "['a', 'b']" into a list of items.
     */
    private List<String> parsePythonList(String listStr) {
        List<String> items = new ArrayList<>();
        if (listStr == null) return items;
        listStr = listStr.trim();
        if (listStr.length() < 2) return items;
        if (listStr.charAt(0) == '[' && listStr.charAt(listStr.length() - 1) == ']') {
            listStr = listStr.substring(1, listStr.length() - 1);
        }
        if (listStr.trim().isEmpty()) return items;
        for (String s : listStr.split(", ")) {
            items.add(s.trim());
        }
        return items;
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
        if (id.contains("object has no attribute 'id'")) {
            id = evaluateExpression(value, String.format("id(%s)", valueName));
        }

        return id;
    }

    /**
     * Returns the collected variables map.
     *
     * @return a map where the key is the variable ID and the value is a list containing the name, type, current value, and scope
     */
    public Map<String, List<String>> getVariables() {
        return variables;
    }

}
