package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.jetbrains.python.debugger.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility class for working with the debugger and extracting stack frames.
 * Provides methods to retrieve all stack frames from a given debugging session.
 *
 * @author julian
 * @version 0.3.0
 * @since 0.1.0
 */
public class DebuggerUtils {

    private static final Logger LOGGER = Logger.getInstance(DebuggerUtils.class);


    private static final int INITIAL_FRAME_INDEX = 1;

    /**
     * Retrieves stack frames for each thread separately.
     * Returns a map where each key is a {@link PyThreadInfo} and the value is the list of {@link PyStackFrame}
     * belonging to that thread.
     *
     * @param debugSession the current debugging session
     * @return a map of thread info objects to their respective lists of stack frames
     */
    public static Map<PyThreadInfo, List<PyStackFrame>> getStackFramesPerThread(XDebugSession debugSession) {
        PyDebugProcess debugProcess = (PyDebugProcess) debugSession.getDebugProcess();
        Collection<PyThreadInfo> threadInfos = debugProcess.getThreads();

        Map<PyThreadInfo, List<PyStackFrame>> perThreadFrames = new HashMap<>();

        // Iterate through each thread, extract its frames, and store them in the map
        for (PyThreadInfo threadInfo : threadInfos) {
            // Prüfen, ob der Thread suspended ist
            if (threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                PyExecutionStack executionStack = new PyExecutionStack(debugProcess, threadInfo);
                List<PyStackFrame> stackFrames = extractStackFrames(executionStack);
                perThreadFrames.put(threadInfo, stackFrames);
            } else if (threadInfo.getState() == PyThreadInfo.State.RUNNING) {
                LOGGER.debug("Still running thread: " + threadInfo.getName()
                        + " (state=" + threadInfo.getState() + ")");
            } else {
                LOGGER.debug("Skipping stack frames for thread: " + threadInfo.getName()
                        + " (state=" + threadInfo.getState() + ")");
            }
        }

        return perThreadFrames;
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
