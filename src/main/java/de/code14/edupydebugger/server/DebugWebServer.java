package de.code14.edupydebugger.server;

import com.intellij.openapi.diagnostic.Logger;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * A simple HTTP server for serving static files during the debugging process.
 * This server is used to provide necessary static resources, such as HTML, CSS, and JavaScript files,
 * to the client (typically a browser-based frontend) during a debugging session.
 * <p>
 * The server is built using the Grizzly framework and listens on a specific port (default: 8026).
 * It serves static content from the "/static/" directory within the classpath.
 * </p>
 *
 * <p>
 * The server is managed using static methods for starting, stopping, and checking the running status.
 * </p>
 *
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebugWebServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebServer.class);

    private HttpServer httpServer;
    private boolean running = false;

    // Singleton instance
    private static DebugWebServer INSTANCE;

    private DebugWebServer() {}

    /**
     * Constructor for injecting a custom HttpServer instance (used for dependency injection)
     */
    public DebugWebServer(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    /**
     * Returns the singleton instance of the DebugWebSocketServer.
     *
     * @return The singleton instance.
     */
    public static DebugWebServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWebServer();
        }
        return INSTANCE;
    }

    /**
     * Starts the HTTP server to serve static content.
     * The server listens on localhost at port 8026.
     * <p>
     * If the server is successfully started, a log message is generated.
     * </p>
     */
    public synchronized void startWebServer() {
        if (running) {
            LOGGER.warn("Web server is already running");
            return;
        }

        if (httpServer == null) {
            // If no server has been injected, create a default HttpServer
            httpServer = new HttpServer();
            NetworkListener networkListener = new NetworkListener("view", "localhost", 8026);
            httpServer.addListener(networkListener);

            httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(
                    DebugWebServer.class.getClassLoader(), "/static/"
            ));
        }

        try {
            httpServer.start();
            LOGGER.info("Web server started on port 8026");
            running = true;
        } catch (Exception e) {
            LOGGER.error("Failed to start web server", e);
            running = false;
        }
    }

    /**
     * Stops the HTTP server if it is currently running.
     * The server is shut down immediately, and the running status is updated.
     */
    public synchronized void stopWebServer() {
        if (httpServer != null && running) {
            httpServer.shutdownNow();
            running = false;
            LOGGER.info("Web server stopped");
        } else {
            LOGGER.warn("Web server is not running");
        }
    }

    /**
     * Checks whether the HTTP server is currently running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Setter for injecting an HttpServer instance (useful for testing).
     */
    public void setHttpServer(HttpServer httpServer) {
        this.httpServer = httpServer;
    }
}
