package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 13.08.24
 */
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
        when(pyDebugProcess.getSession()).thenReturn(xDebugSession);
        when(pyDebugProcess.getProcessHandler()).thenReturn(processHandler);
        when(toolWindowManager.getToolWindow(anyString())).thenReturn(toolWindow);
        when(toolWindow.isAvailable()).thenReturn(true);

        // Initialize the DebugProcessListener (Spy on DebugProcessListener to override the instantiation of ConsoleOutputListener)
        debugProcessListener = Mockito.spy(new DebugProcessListener(project));

        // Override the instantiation of ConsoleOutputListener in the processStarted method
        doReturn(consoleOutputListener).when(debugProcessListener).createConsoleOutputListener(any(ProcessHandler.class));
    }

    @Test
    public void testProcessStarted() {
        // Mocking static methods for DebugWebSocketServer, DebugWebServer, and DebugServerEndpoint
        try (MockedStatic<DebugWebSocketServer> webSocketServerMock = Mockito.mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = Mockito.mockStatic(DebugWebServer.class);
             MockedStatic<DebugServerEndpoint> debugServerEndpointMock = Mockito.mockStatic(DebugServerEndpoint.class)) {

            // Mock the behavior of the singletons
            DebugWebSocketServer mockWebSocketServer = mock(DebugWebSocketServer.class);
            DebugWebServer mockWebServer = mock(DebugWebServer.class);

            // Mock the static singleton instance getter methods
            webSocketServerMock.when(DebugWebSocketServer::getInstance).thenReturn(mockWebSocketServer);
            webServerMock.when(DebugWebServer::getInstance).thenReturn(mockWebServer);

            // Set up mock behavior for starting servers
            when(mockWebSocketServer.isRunning()).thenReturn(false);
            when(mockWebServer.isRunning()).thenReturn(false);

            // Act: simulate starting the process
            debugProcessListener.processStarted(pyDebugProcess);

            // Verify that startWebSocketServer and startWebServer were called
            verify(mockWebSocketServer, times(1)).startWebSocketServer();
            verify(mockWebServer, times(1)).startWebServer();

            // Verify that DebugServerEndpoint.setDebugProcess() was called
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setDebugProcess(pyDebugProcess), times(1));

            // Verify that attachConsoleListeners() was called on the Spy
            verify(consoleOutputListener, times(1)).attachConsoleListeners();
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

