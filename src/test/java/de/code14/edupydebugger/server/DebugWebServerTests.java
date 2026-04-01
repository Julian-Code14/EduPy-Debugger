package de.code14.edupydebugger.server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;


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

    @Test
    public void testStopWebServerWhenNotRunning() {
        // Server wurde nie gestartet → sollte nichts tun
        webServer.stopWebServer();

        // shutdownNow darf nicht aufgerufen werden
        verify(mockHttpServer, never()).shutdownNow();
        assertFalse(webServer.isRunning());
    }

    @Test
    public void testStartWebServerFailureDoesNotSetRunning() throws Exception {
        // start() wirft Exception → im IntelliJ-Testumfeld führt LOGGER.error zu AssertionError
        doThrow(new RuntimeException("boom")).when(mockHttpServer).start();

        try {
            webServer.startWebServer();
            fail("AssertionError expected due to Logger.error");
        } catch (AssertionError expected) {
            // erwartet: Logger.error signalisiert Fehler als AssertionError
        }

        verify(mockHttpServer, times(1)).start();
        assertFalse(webServer.isRunning());
    }
}
