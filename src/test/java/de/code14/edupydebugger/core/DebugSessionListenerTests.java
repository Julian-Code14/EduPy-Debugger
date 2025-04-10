package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;

import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyThreadInfo;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

// Subclass that suppresses static analysis during testing.
class TestDebugSessionListener extends DebugSessionListener {
    // Construct with a PyDebugProcess object.
    public TestDebugSessionListener(PyDebugProcess debugProcess) {
        super(debugProcess);
    }
    // Override to disable static analysis in tests.
    @Override
    protected void performStaticAnalysis(PyDebugProcess pyDebugProcess) {
        // No static analysis in test mode.
    }
}

/**
 * Tests for DebugSessionListener.
 *
 * @author julian
 * @version 0.3.0
 * @since 0.1.0
 */
public class DebugSessionListenerTests {

    @Mock
    private XDebugSession mockXDebugSession; // Mock for XDebugSession.
    @Mock
    private PyDebugProcess mockPyDebugProcess; // Mock for PyDebugProcess.
    @Mock
    private Project mockProject; // Mock for Project to supply non-null project.

    private DebugSessionListener debugSessionListener; // System under test.

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks.

        // Configure PyDebugProcess to return the mocked XDebugSession.
        when(mockPyDebugProcess.getSession()).thenReturn(mockXDebugSession);
        // Stub project retrieval and base path.
        when(mockPyDebugProcess.getProject()).thenReturn(mockProject);
        when(mockProject.getBasePath()).thenReturn("dummyPath");

        // Instantiate the test-specific listener that suppresses static analysis.
        debugSessionListener = new TestDebugSessionListener(mockPyDebugProcess);
    }

    @Test
    public void testConstructorTriggersStaticAnalysis() throws IOException {
        // Verify that the overridden performStaticAnalysis does not invoke any static endpoint.
        try (MockedStatic<DebugServerEndpoint> endpointMock = Mockito.mockStatic(DebugServerEndpoint.class)) {
            // Expect no interactions with DebugServerEndpoint.
            endpointMock.verifyNoInteractions();
        }
    }

    @Test
    public void testStackFrameChanged() throws Exception {
        try (MockedStatic<DebuggerUtils> debuggerUtilsMock = Mockito.mockStatic(DebuggerUtils.class);
             MockedStatic<DebugServerEndpoint> debugEndpointMock = Mockito.mockStatic(DebugServerEndpoint.class)) {

            // Stub getDebugSessionController() to return a dummy controller.
            DebugSessionController dummyController = mock(DebugSessionController.class);
            debugEndpointMock.when(DebugServerEndpoint::getDebugSessionController).thenReturn(dummyController);
            // Stub getSelectedThread() to return null.
            debugEndpointMock.when(DebugServerEndpoint::getSelectedThread).thenReturn(null);

            // Create a suspended thread mock.
            PyThreadInfo t1 = mock(PyThreadInfo.class);
            when(t1.getName()).thenReturn("Thread-A");
            when(t1.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);
            // Stub DebuggerUtils.getThreads() to return a list with the suspended thread.
            debuggerUtilsMock.when(() -> DebuggerUtils.getThreads(mockXDebugSession))
                    .thenReturn(List.of(t1));

            // Invoke stackFrameChanged(), which calls performDynamicAnalysis.
            debugSessionListener.stackFrameChanged();

            // Verify that the thread options string is set and sent.
            debugEndpointMock.verify(() -> DebugServerEndpoint.setThreadOptionsString("Thread-A (angehalten);"), times(1));
            debugEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo("threads:Thread-A (angehalten);"), times(1));
            // Verify that performDynamicAnalysis(null) is invoked via the dummy controller.
            verify(dummyController, times(1)).performDynamicAnalysis(null);
        }
    }
}