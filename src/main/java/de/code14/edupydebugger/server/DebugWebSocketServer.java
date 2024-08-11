package de.code14.edupydebugger.server;


import com.intellij.openapi.diagnostic.Logger;
import org.glassfish.tyrus.server.Server;

/**
 * Manages the WebSocket server used for debugging communication.
 * This server facilitates real-time communication between the debugger and the client,
 * allowing for the exchange of debug information, control commands, and other data.
 * <p>
 * The server is built using the Tyrus framework and listens on a specific port (default: 8025).
 * It hosts WebSocket endpoints defined within the application, specifically the {@link DebugServerEndpoint}.
 * </p>
 *
 * <p>
 * The class provides static methods for starting, stopping, and checking the running status of the server.
 * It also handles context classloader management to ensure compatibility within the IntelliJ platform.
 * </p>
 *
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class DebugWebSocketServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebSocketServer.class);

    private static Server server;
    private static boolean running = false;

    /**
     * Starts the WebSocket server on localhost at port 8025.
     * <p>
     * The server is initialized with the {@link DebugServerEndpoint} class, which handles incoming WebSocket connections.
     * The context classloader is temporarily switched to ensure that the server starts correctly within the IntelliJ platform.
     * </p>
     */
    public static void startWebSocketServer() {
        server = new Server("localhost", 8025, "/websockets", null, DebugServerEndpoint.class);

        // Context ClassLoader Handling
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        ClassLoader pluginClassLoader = DebugWebSocketServer.class.getClassLoader();

        try {
            currentThread.setContextClassLoader(pluginClassLoader);
            server.start();
            LOGGER.info("WebSocket server started on port " + server.getPort());
            running = true;
        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Stops the WebSocket server if it is currently running.
     * The server is gracefully shut down, and the running status is updated.
     */
    public static void stopWebSocketServer() {
        if (server != null && running) {
            server.stop();
            running = false;
        }
    }

    /**
     * Checks whether the WebSocket server is currently running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise.
     */
    public static boolean isRunning() {
        return running;
    }

}
