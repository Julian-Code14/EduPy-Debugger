package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.swing.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class EduPyProjectManagerListenerTests {

    private EduPyProjectManagerListener listener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new EduPyProjectManagerListener();
    }

    @Test
    public void testProjectClosing() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<SwingUtilities> swingUtilitiesMock = mockStatic(SwingUtilities.class);
             MockedStatic<DebugWebSocketServer> webSocketServerMock = mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = mockStatic(DebugWebServer.class);
             MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = mockStatic(DebuggerToolWindowFactory.class)) {

            // Mock WebSocketServer and WebServer singletons
            DebugWebSocketServer mockWebSocketServer = mock(DebugWebSocketServer.class);
            DebugWebServer mockWebServer = mock(DebugWebServer.class);

            // Mock the getInstance calls to return the mocks
            webSocketServerMock.when(DebugWebSocketServer::getInstance).thenReturn(mockWebSocketServer);
            webServerMock.when(DebugWebServer::getInstance).thenReturn(mockWebServer);

            // Mock isRunning to return true
            when(mockWebSocketServer.isRunning()).thenReturn(true);
            when(mockWebServer.isRunning()).thenReturn(true);

            // Mock SwingUtilities.invokeLater to run the task immediately
            swingUtilitiesMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    });

            // Act
            listener.projectClosing(mockProject);

            // Verify that WebSocket and Web servers were stopped
            verify(mockWebSocketServer, times(1)).stopWebSocketServer();
            verify(mockWebServer, times(1)).stopWebServer();

            // Verify that the JBCefBrowser was closed
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::closeJBCefBrowser, times(1));
        }
    }

    @Test
    public void testProjectClosingWhenServersNotRunning() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<SwingUtilities> swingUtilitiesMock = mockStatic(SwingUtilities.class);
             MockedStatic<DebugWebSocketServer> webSocketServerMock = mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = mockStatic(DebugWebServer.class);
             MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = mockStatic(DebuggerToolWindowFactory.class)) {

            // Mock WebSocketServer and WebServer singletons
            DebugWebSocketServer mockWebSocketServer = mock(DebugWebSocketServer.class);
            DebugWebServer mockWebServer = mock(DebugWebServer.class);

            // Mock the getInstance calls to return the mocks
            webSocketServerMock.when(DebugWebSocketServer::getInstance).thenReturn(mockWebSocketServer);
            webServerMock.when(DebugWebServer::getInstance).thenReturn(mockWebServer);

            // Mock isRunning to return false
            when(mockWebSocketServer.isRunning()).thenReturn(false);
            when(mockWebServer.isRunning()).thenReturn(false);

            // Mock SwingUtilities.invokeLater to run the task immediately
            swingUtilitiesMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    });

            // Act
            listener.projectClosing(mockProject);

            // Verify that WebSocket and Web servers were not stopped since they were not running
            verify(mockWebSocketServer, never()).stopWebSocketServer();
            verify(mockWebServer, never()).stopWebServer();

            // Verify that the JBCefBrowser was still closed
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::closeJBCefBrowser, times(1));
        }
    }
}

