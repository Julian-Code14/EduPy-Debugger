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
 *
 * @author julian
 * @version 1.0
 * @since 11.08.24
 */
public class VariableAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(VariableAnalyzer.class);

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

                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    String id = evaluateExpression(value, "__builtins__.id(" + value.getName() + ")");
                    variables.put(id, new ArrayList<>(Arrays.asList(value.getName(), value.getType(), value.getValue(), determineScope(value))));
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
            LOGGER.error("Error evaluating expression: " + expression, e);
            return "";
        }
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
