package de.code14.edupydebugger.debugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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


    // Muster: Key: ID, Values -> [Name, Typ, aktueller Wert, Scope] ggf. zu erweitern um global/statisch/etc.
    private Map<String, List<String>> variables = new HashMap<>();

    private List<PyStackFrame> pyStackFrames = new ArrayList<>();


    public StackFrameAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.pyStackFrames = pyStackFrames;
    }

    public void analyzeFrames() {
        variables.clear();
        CountDownLatch latch = new CountDownLatch(this.pyStackFrames.size());

        LOGGER.debug("Anzahl der detektierten PyStackFrames: " + this.pyStackFrames.size());
        for (PyStackFrame frame : this.pyStackFrames) {
            collectAttributes(frame, latch);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private void collectAttributes(PyStackFrame pyStackFrame, CountDownLatch latch) {
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
                    System.out.println("collectAttributes: " + id + " -> " + value.getName() + ": " + value.getValue() + " (" + value.getType() + ") [" + determineScope(value) + "]");
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


    public Map<String, List<String>> getVariables() {
        return variables;
    }

}
