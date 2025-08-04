package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.jetbrains.python.debugger.*;
import org.junit.*;
import org.mockito.MockedConstruction;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class DebuggerUtilsTests {

    private PyDebugProcess mockProcess;
    private XDebugSession mockSession;
    private PyThreadInfo suspendedThread;
    private PyThreadInfo runningThread;

    @Before
    public void setUp() {
        mockProcess = mock(PyDebugProcess.class);
        mockSession = mock(XDebugSession.class);

        // ----- PyThreadInfo lassen sich ­einfach mocken ---------------
        suspendedThread = mock(PyThreadInfo.class);
        when(suspendedThread.getName()).thenReturn("MainThread");
        when(suspendedThread.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);

        runningThread = mock(PyThreadInfo.class);
        when(runningThread.getName()).thenReturn("Worker");
        when(runningThread.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        when(mockSession.getDebugProcess()).thenReturn(mockProcess);
        when(mockProcess.getThreads()).thenReturn(List.of(suspendedThread, runningThread));
    }

    @Test
    public void testGetThreadsReturnsAllThreads() {
        List<PyThreadInfo> result = DebuggerUtils.getThreads(mockSession);
        assertEquals(2, result.size());
        assertTrue(result.containsAll(List.of(suspendedThread, runningThread)));
    }

    @Test
    public void testGetStackFramesPerThreadCollectsOnlySuspendedThreads() {
        // Jede neue PyExecutionStack hooken
        try (MockedConstruction<PyExecutionStack> mocked =
                     mockConstruction(PyExecutionStack.class, (stack, ctx) -> {
                         PyStackFrame fakeFrame = mock(PyStackFrame.class);
                         when(stack.getTopFrame()).thenReturn(fakeFrame);
                         // computeStackFrames(...) ruft sofort den Container-Callback
                         doAnswer(inv -> {
                             XExecutionStack.XStackFrameContainer c = inv.getArgument(1);
                             c.addStackFrames(List.of(fakeFrame), true);
                             return null;
                         }).when(stack).computeStackFrames(anyInt(), any());
                     })) {

            Map<PyThreadInfo, List<PyStackFrame>> frames =
                    DebuggerUtils.getStackFramesPerThread(mockSession);

            assertEquals(1, frames.size());                      // nur der SUSPENDED-Thread
            assertTrue(frames.containsKey(suspendedThread));
            assertFalse(frames.get(suspendedThread).isEmpty());   // ≥ 1 Frame reicht
        }
    }
}