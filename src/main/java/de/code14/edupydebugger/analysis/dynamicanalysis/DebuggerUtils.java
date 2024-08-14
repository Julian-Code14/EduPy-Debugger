package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyExecutionStack;
import com.jetbrains.python.debugger.PyStackFrame;
import com.jetbrains.python.debugger.PyThreadInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility class for working with the debugger and extracting stack frames.
 * Provides methods to retrieve all stack frames from a given debugging session.
 *
 * @author julian
 * @version 1.0
 * @since 17.07.24
 */
public class DebuggerUtils {

    private static final Logger LOGGER = Logger.getInstance(DebuggerUtils.class);


    private static final int INITIAL_FRAME_INDEX = 1;

    /**
     * Retrieves all stack frames from the current debugging session.
     * This method iterates over all threads in the current session, extracts the stack frames,
     * and returns them as a list of {@link PyStackFrame} objects.
     *
     * @param debugSession the current debugging session
     * @return a list of all stack frames across all threads
     */
    public static List<PyStackFrame> getAllStackFrames(XDebugSession debugSession) {
        // Get the debug process associated with the current session
        PyDebugProcess debugProcess = (PyDebugProcess) debugSession.getDebugProcess();

        // Retrieve all thread information from the debug process
        Collection<PyThreadInfo> threadInfos = debugProcess.getThreads();

        // List to store all stack frames
        List<PyStackFrame> allStackFrames = new CopyOnWriteArrayList<>();

        // Iterate through each thread to extract stack frames
        for (PyThreadInfo threadInfo : threadInfos) {
            PyExecutionStack executionStack = new PyExecutionStack(debugProcess, threadInfo);
            List<PyStackFrame> stackFrames = extractStackFrames(executionStack);
            allStackFrames.addAll(stackFrames);
        }

        return allStackFrames;
    }

    /**
     * Extracts all stack frames from a given execution stack.
     * This method starts with the top frame and asynchronously retrieves the remaining frames.
     *
     * @param executionStack the execution stack to extract frames from
     * @return a list of {@link PyStackFrame} objects representing the stack frames in the execution stack
     */
    private static @NotNull List<PyStackFrame> extractStackFrames(PyExecutionStack executionStack) {
        List<PyStackFrame> stackFrames = new CopyOnWriteArrayList<>();

        // Add the top stack frame (most recent frame), ensuring it's not null
        PyStackFrame topFrame = executionStack.getTopFrame();
        if (topFrame != null) {
            stackFrames.add(topFrame);
        }

        // Asynchronously retrieve the remaining stack frames
        executionStack.computeStackFrames(INITIAL_FRAME_INDEX, new XExecutionStack.XStackFrameContainer() {
            @Override
            public void addStackFrames(@NotNull List<? extends XStackFrame> xFrames, boolean last) {
                for (XStackFrame frame : xFrames) {
                    if (frame instanceof PyStackFrame) {
                        stackFrames.add((PyStackFrame) frame);
                    }
                }
            }

            @Override
            public void errorOccurred(@NotNull String errorMessage) {
                // Log the error or handle it appropriately
                LOGGER.error("Error occurred while fetching stack frames: " + errorMessage);
            }

            @Override
            public boolean isObsolete() {
                // Implement logic to determine if the container is obsolete, if necessary
                return false;
            }
        });

        return stackFrames;
    }

}
