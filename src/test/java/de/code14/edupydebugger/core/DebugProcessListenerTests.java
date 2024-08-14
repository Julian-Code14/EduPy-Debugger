package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @Mock private DebuggerToolWindowFactory debuggerToolWindowFactory;
    private DebugProcessListener debugProcessListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(pyDebugProcess.getSession()).thenReturn(xDebugSession);
        when(toolWindowManager.getToolWindow(anyString())).thenReturn(toolWindow);
        when(toolWindow.isAvailable()).thenReturn(true);
        debugProcessListener = new DebugProcessListener(project);
    }

    @Test
    void testProcessStarted() {
        // Mocking static methods
        try (MockedStatic<DebugWebSocketServer> webSocketServerMock = Mockito.mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = Mockito.mockStatic(DebugWebServer.class);
             MockedStatic<DebugServerEndpoint> debugServerEndpointMock = Mockito.mockStatic(DebugServerEndpoint.class)) {

            // Arrange
            webSocketServerMock.when(DebugWebSocketServer::isRunning).thenReturn(false);
            webServerMock.when(DebugWebServer::isRunning).thenReturn(false);
            webSocketServerMock.when(DebugWebSocketServer::startWebSocketServer).then(invocation -> {
                webSocketServerMock.when(DebugWebSocketServer::isRunning).thenReturn(true);
                return null;
            });
            webServerMock.when(DebugWebServer::startWebServer).then(invocation -> {
                webServerMock.when(DebugWebServer::isRunning).thenReturn(true);
                return null;
            });

            // Act
            debugProcessListener.processStarted(pyDebugProcess);

            // Assert
            webSocketServerMock.verify(DebugWebSocketServer::startWebSocketServer, times(1));
            webServerMock.verify(DebugWebServer::startWebServer, times(1));
            debugServerEndpointMock.verify(() -> DebugServerEndpoint.setDebugProcess(pyDebugProcess), times(1));
            // Assert that the servers are running
            assertTrue(DebugWebSocketServer.isRunning(), "WebSocket server should be running");
            assertTrue(DebugWebServer.isRunning(), "Web server should be running");
        }
    }

    @Test
    void testProcessStopped() {
        // Mocking the static method in DebuggerToolWindowFactory
        try (MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = Mockito.mockStatic(DebuggerToolWindowFactory.class)) {

            // Act
            debugProcessListener.processStopped(pyDebugProcess);

            // Assert
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::reloadEduPyDebugger, times(1));
        }
    }

}
