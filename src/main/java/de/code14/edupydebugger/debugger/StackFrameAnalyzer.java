package de.code14.edupydebugger.debugger;

import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.frame.*;
import com.jetbrains.python.debugger.PyDebugValue;
import com.jetbrains.python.debugger.PyStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author julian
 * @version 1.0
 * @since 17.07.24
 */
public class StackFrameAnalyzer {

    // Muster: Key: ID, Values -> [Name, Typ, aktueller Wert] ggf. zu erweitern um global/statisch/etc.
    private Map<String, List<String>> variables = new HashMap<>();

    private List<PyStackFrame> pyStackFrames = new ArrayList<>();


    public StackFrameAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.pyStackFrames = pyStackFrames;
    }

    public void analyzeFrames() {
        variables.clear();

        for (PyStackFrame frame : this.pyStackFrames) {
            collectAttributes(frame);
        }

    }

    private void collectAttributes(PyStackFrame pyStackFrame) {
        pyStackFrame.computeChildren(new XCompositeNode() {

            @Override
            public void addChildren(@NotNull XValueChildrenList children, boolean last) {
                for (int i = 0; i < children.size(); i++) {
                    PyDebugValue value = (PyDebugValue) children.getValue(i);
                    System.out.println("collectAttributes: " + value.getName() + ": " + value.getValue() + " (" + value.getType() + ")");
                    variables.put(value.getId(), new ArrayList<>(Arrays.asList(value.getName(), value.getType(), value.getValue())));
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

    public Map<String, List<String>> getVariables() {
        return variables;
    }

}
