package de.code14.edupydebugger.server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebugWebServerTests {

    private DebugWebServer webServer;
    private HttpServer mockHttpServer;

    @Before
    public void setUp() {
        mockHttpServer = mock(HttpServer.class);  // Mock the HttpServer
        webServer = new DebugWebServer(mockHttpServer);  // Inject the mocked HttpServer
    }

    @Test
    public void testStartWebServerSuccess() throws Exception {
        // Simulate that the server starts successfully
        doNothing().when(mockHttpServer).start();

        // Start the Web server
        webServer.startWebServer();

        // Verify that server.start() was called
        verify(mockHttpServer, times(1)).start();

        // Ensure that the Web server is running
        assertTrue(webServer.isRunning());
    }

    @Test
    public void testStopWebServerSuccess() throws Exception {
        // Simulate starting and stopping the server
        doNothing().when(mockHttpServer).start();
        doNothing().when(mockHttpServer).shutdownNow();

        webServer.startWebServer();
        assertTrue(webServer.isRunning());

        webServer.stopWebServer();
        verify(mockHttpServer, times(1)).shutdownNow();
        assertFalse(webServer.isRunning());
    }

    @Test
    public void testStartWebServerAlreadyRunning() throws Exception {
        // Simulate that the server starts successfully
        doNothing().when(mockHttpServer).start();

        // Start the Web server once
        webServer.startWebServer();
        assertTrue(webServer.isRunning());

        // Try to start the Web server again
        webServer.startWebServer();

        // Verify that server.start() is called only once
        verify(mockHttpServer, times(1)).start();
    }
}
