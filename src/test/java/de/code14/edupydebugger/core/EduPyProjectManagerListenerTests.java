package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.swing.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class EduPyProjectManagerListenerTests {

    private EduPyProjectManagerListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new EduPyProjectManagerListener();
    }

    @Test
    void testProjectClosing() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<SwingUtilities> swingUtilitiesMock = mockStatic(SwingUtilities.class);
             MockedStatic<DebugWebSocketServer> webSocketServerMock = mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = mockStatic(DebugWebServer.class);
             MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = mockStatic(DebuggerToolWindowFactory.class)) {

            // Mocking WebSocket Server
            webSocketServerMock.when(DebugWebSocketServer::isRunning).thenReturn(true);

            // Mocking Web Server
            webServerMock.when(DebugWebServer::isRunning).thenReturn(true);

            // Mocking SwingUtilities.invokeLater to run immediately
            swingUtilitiesMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    });

            // Act
            listener.projectClosing(mockProject);

            // Assert that WebSocket Server stop method was called
            webSocketServerMock.verify(() -> DebugWebSocketServer.stopWebSocketServer(), times(1));

            // Assert that Web Server stop method was called
            webServerMock.verify(() -> DebugWebServer.stopWebServer(), times(1));

            // Assert that the JBCefBrowser was closed
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::closeJBCefBrowser, times(1));
        }
    }

    @Test
    void testProjectClosingWhenServersNotRunning() {
        Project mockProject = mock(Project.class);

        try (MockedStatic<SwingUtilities> swingUtilitiesMock = mockStatic(SwingUtilities.class);
             MockedStatic<DebugWebSocketServer> webSocketServerMock = mockStatic(DebugWebSocketServer.class);
             MockedStatic<DebugWebServer> webServerMock = mockStatic(DebugWebServer.class);
             MockedStatic<DebuggerToolWindowFactory> toolWindowFactoryMock = mockStatic(DebuggerToolWindowFactory.class)) {

            // Mocking WebSocket Server to return false for isRunning
            webSocketServerMock.when(DebugWebSocketServer::isRunning).thenReturn(false);

            // Mocking Web Server to return false for isRunning
            webServerMock.when(DebugWebServer::isRunning).thenReturn(false);

            // Mocking SwingUtilities.invokeLater to run immediately
            swingUtilitiesMock.when(() -> SwingUtilities.invokeLater(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    });

            // Act
            listener.projectClosing(mockProject);

            // Assert that WebSocket Server stop method was not called since it's not running
            webSocketServerMock.verify(() -> DebugWebSocketServer.stopWebSocketServer(), never());

            // Assert that Web Server stop method was not called since it's not running
            webServerMock.verify(() -> DebugWebServer.stopWebServer(), never());

            // Assert that the JBCefBrowser was still closed
            toolWindowFactoryMock.verify(DebuggerToolWindowFactory::closeJBCefBrowser, times(1));
        }
    }
}
