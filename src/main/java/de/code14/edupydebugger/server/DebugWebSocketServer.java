package de.code14.edupydebugger.server;


import com.intellij.openapi.diagnostic.Logger;
import org.glassfish.tyrus.server.Server;

/**
 * Singleton class that manages the WebSocket server used for debugging communication.
 * This server facilitates real-time communication between the debugger and the client,
 * allowing for the exchange of debug information, control commands, and other data.
 * <p>
 * The server is built using the Tyrus framework and listens on a specific port (default: 8025).
 * It hosts WebSocket endpoints defined within the application, specifically the {@link DebugServerEndpoint}.
 * </p>
 */
public class DebugWebSocketServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebSocketServer.class);

    private Server server;
    private boolean running = false;

    // Singleton instance
    private static DebugWebSocketServer INSTANCE;

    private DebugWebSocketServer() {}

    /**
     * Dependency injection of the server instance
     */
    public DebugWebSocketServer(Server server) {
        this.server = server;
    }

    /**
     * Returns the singleton instance of the DebugWebSocketServer.
     *
     * @return The singleton instance.
     */
    public static DebugWebSocketServer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugWebSocketServer();
        }
        return INSTANCE;
    }

    /**
     * Starts the WebSocket server on localhost at port 8025.
     * <p>
     * The server is initialized with the {@link DebugServerEndpoint} class, which handles incoming WebSocket connections.
     * The context classloader is temporarily switched to ensure that the server starts correctly within the IntelliJ platform.
     * </p>
     */
    public synchronized void startWebSocketServer() {
        if (server == null) {
            this.server = new Server("localhost", 8025, "/websockets", null, DebugServerEndpoint.class);
        }

        if (running) {
            LOGGER.warn("WebSocket server is already running.");
            return;
        }

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
            LOGGER.error("Failed to start WebSocket server", e);
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Stops the WebSocket server if it is currently running.
     * The server is gracefully shut down, and the running status is updated.
     */
    public synchronized void stopWebSocketServer() {
        if (running) {
            server.stop();
            running = false;
            LOGGER.info("WebSocket server stopped.");
        } else {
            LOGGER.warn("WebSocket server is not running.");
        }
    }

    /**
     * Checks whether the WebSocket server is currently running.
     *
     * @return {@code true} if the server is running, {@code false} otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    public void setServer(Server server) {
        this.server = server;
    }

}
