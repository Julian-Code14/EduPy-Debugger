package de.code14.edupydebugger.debugger;

import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyExecutionStack;
import com.jetbrains.python.debugger.PyStackFrame;
import com.jetbrains.python.debugger.PyThreadInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author julian
 * @version 1.0
 * @since 17.07.24
 */
public class DebuggerUtils {

    public static List<PyStackFrame> getAllStackFrames(XDebugSession debugSession) {
        PyDebugProcess debugProcess = (PyDebugProcess) debugSession.getDebugProcess();
        Collection<PyThreadInfo> threadInfos = debugProcess.getThreads();
        List<PyStackFrame> allStackFrames = new ArrayList<>();

        for (PyThreadInfo threadInfo : threadInfos) {
            PyExecutionStack executionStack = new PyExecutionStack(debugProcess, threadInfo);
            List<PyStackFrame> stackFrames = extractStackFrames(executionStack);
            allStackFrames.addAll(stackFrames);
        }

        return allStackFrames;
    }

    private static List<PyStackFrame> extractStackFrames(PyExecutionStack executionStack) {
        List<PyStackFrame> stackFrames = new ArrayList<>();
        stackFrames.add(executionStack.getTopFrame());

        // Rufen Sie die restlichen Frames asynchron ab
        executionStack.computeStackFrames(1, new XExecutionStack.XStackFrameContainer() {
            @Override
            public void addStackFrames(List<? extends XStackFrame> xFrames, boolean last) {
                for (XStackFrame frame : xFrames) {
                    stackFrames.add((PyStackFrame) frame);
                }
            }

            @Override
            public void errorOccurred(String errorMessage) {
                // Fehlerbehandlung, falls nötig
            }

            @Override
            public boolean isObsolete() {
                return false; // Implementieren Sie eine Logik, um veraltete Container zu erkennen, falls nötig
            }
        });

        return stackFrames;
    }

}
