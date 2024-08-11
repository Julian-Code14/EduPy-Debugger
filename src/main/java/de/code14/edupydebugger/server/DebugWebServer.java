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
 * @version 1.0
 * @since 03.07.24
 */
public class DebugWebServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebServer.class);

    private static HttpServer httpServer;
    private static boolean running = false;

    /**
     * Starts the HTTP server to serve static content.
     * The server listens on localhost at port 8026.
     * <p>
     * If the server is successfully started, a log message is generated.
     * </p>
     */
    public static void startWebServer() {
        httpServer = new HttpServer();
        final NetworkListener networkListener = new NetworkListener("view", "localhost", 8026);
        httpServer.addListener(networkListener);

        httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(
                DebugWebServer.class.getClassLoader(), "/static/"
        ));

        try {
            httpServer.start();
            LOGGER.info("Web server started on port 8026");
            running = true;
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    /**
     * Stops the HTTP server if it is currently running.
     * The server is shut down immediately, and the running status is updated.
     */
    public static void stopWebServer() {
        if (httpServer != null && httpServer.isStarted()) {
            httpServer.shutdownNow();
            running = false;
        }
    }

    /**
     * Checks whether the HTTP server is currently running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise.
     */
    public static boolean isRunning() {
        return running;
    }
}
