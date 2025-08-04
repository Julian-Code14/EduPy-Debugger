package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class DebugProcessListenerTests {

    @Mock private Project project;
    @Mock private XDebugSession xDebugSession;
    @Mock private ToolWindowManager toolWindowManager;
    @Mock private ToolWindow toolWindow;
    @Mock private PyDebugProcess pyDebugProcess;
    @Mock private ProcessHandler processHandler;
    @Mock private DebuggerToolWindowFactory debuggerToolWindowFactory;
    @Mock private ConsoleOutputListener consoleOutputListener;

    private DebugProcessListener debugProcessListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stub das pyDebugProcess-Mock, damit es den erwarteten XDebugSession und ProcessHandler liefert.
        when(pyDebugProcess.getSession()).thenReturn(xDebugSession);
        when(pyDebugProcess.getProcessHandler()).thenReturn(processHandler);
        // Hier hinzufügen: Wenn pyDebugProcess.getProject() aufgerufen wird, soll der project-Mock zurückgegeben werden.
        when(pyDebugProcess.getProject()).thenReturn(project);

        // Stub the project to return a dummy base path.
        when(project.getBasePath()).thenReturn("dummyPath");
        // Ensure that when ToolWindowManager.getInstance(project) is called, the project returns our mock ToolWindowManager.
        when(project.getService(ToolWindowManager.class)).thenReturn(toolWindowManager);

        // Set up mocks for ToolWindowManager and ToolWindow.
        when(toolWindowManager.getToolWindow(anyString())).thenReturn(toolWindow);
        when(toolWindow.isAvailable()).thenReturn(true);

        // Initialize the DebugProcessListener as a spy so that we can override the creation of ConsoleOutputListener.
        debugProcessListener = Mockito.spy(new DebugProcessListener(project));
        doReturn(consoleOutputListener).when(debugProcessListener)
                .createConsoleOutputListener(any(ProcessHandler.class));
    }

    @Test
    public void testProcessStarted() {
        try (MockedStatic<DebugWebSocketServer> webSocketServerMock = Mockito.mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = Mockito.mockStatic(DebugWebServer.class);
             MockedStatic<DebugServerEndpoint> debugServerEndpointMock = Mockito.mockStatic(DebugServerEndpoint.class);
             // Intercept the construction of DebugSessionListener to prevent static analysis from running.
             MockedConstruction<DebugSessionListener> listenerConstruction =
                     Mockito.mockConstruction(DebugSessionListener.class, (mockListener, context) -> {
                         // Optionally stub methods on the mock if needed.
                     })) {

            // Set up the singleton mocks:
            DebugWebSocketServer mockWebSocketServer = mock(DebugWebSocketServer.class);
            DebugWebServer mockWebServer = mock(DebugWebServer.class);
            webSocketServerMock.when(DebugWebSocketServer::getInstance).thenReturn(mockWebSocketServer);
            webServerMock.when(DebugWebServer::getInstance).thenReturn(mockWebServer);

            // Ensure that both servers are reported as not running so that start...() will be called.
            when(mockWebSocketServer.isRunning()).thenReturn(false);
            when(mockWebServer.isRunning()).thenReturn(false);

            // Act: simulate starting the process.
            debugProcessListener.processStarted(pyDebugProcess);

            // Verify that the WebSocket and HTTP servers are started.
            verify(mockWebSocketServer, times(1)).startWebSocketServer();
            verify(mockWebServer, times(1)).startWebServer();

            // Verify that the DebugServerEndpoint methods are called to set the debug process and process handler.
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setDebugProcess(pyDebugProcess), times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setProcessHandler(processHandler), times(1));

            // Verify that attachConsoleListeners() is called on the spy instance of ConsoleOutputListener.
            verify(consoleOutputListener, times(1)).attachConsoleListeners();

            // Finally, verify that a DebugSessionListener instance was constructed.
            assertFalse("A new DebugSessionListener instance should have been constructed",
                    listenerConstruction.constructed().isEmpty());
        }
    }

    @Test
    public void testProcessStopped() {
        // Mocking the static method in DebuggerToolWindowFactory
        try (MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = Mockito.mockStatic(DebuggerToolWindowFactory.class)) {

            // Act: simulate stopping the process
            debugProcessListener.processStopped(pyDebugProcess);

            // Assert: verify that reloadEduPyDebugger was called once
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::reloadEduPyDebugger, times(1));
        }
    }

}

