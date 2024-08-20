package de.code14.edupydebugger.server;

import org.glassfish.tyrus.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class DebugWebSocketServerTests {

    private DebugWebSocketServer webSocketServer;
    private Server mockServer;

    @BeforeEach
    void setUp() {
        mockServer = mock(Server.class);
        webSocketServer = new DebugWebSocketServer(mockServer);
    }

    @Test
    void testStartWebSocketServerSuccess() throws Exception {
        // Simulate that the server starts successfully
        doNothing().when(mockServer).start();

        // Start the WebSocket server
        webSocketServer.startWebSocketServer();

        // Verify that server.start() was called
        verify(mockServer, times(1)).start();

        // Ensure that the WebSocket server is running
        assertTrue(webSocketServer.isRunning());
    }

    @Test
    void testStopWebSocketServerSuccess() throws Exception {
        // Simulate starting and stopping the server
        doNothing().when(mockServer).start();
        doNothing().when(mockServer).stop();

        webSocketServer.startWebSocketServer();
        assertTrue(webSocketServer.isRunning());

        webSocketServer.stopWebSocketServer();
        verify(mockServer, times(1)).stop();
        assertFalse(webSocketServer.isRunning());
    }

    @Test
    void testStopWebSocketServerWhenNotRunning() {
        // Stop the server when it's not running
        webSocketServer.stopWebSocketServer();

        // Verify that server.stop() was never called since it wasn't running
        verify(mockServer, never()).stop();

        // Ensure that the WebSocket server is not running
        assertFalse(webSocketServer.isRunning());
    }

    @Test
    void testGetInstance() {
        // Verify that the singleton instance is not null and remains the same across multiple calls
        DebugWebSocketServer instance1 = DebugWebSocketServer.getInstance();
        DebugWebSocketServer instance2 = DebugWebSocketServer.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

}
