package de.code14.edupydebugger.server;


import com.intellij.openapi.diagnostic.Logger;
import org.glassfish.tyrus.server.Server;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class DebugWebSocketServer {

    private static final Logger LOGGER = Logger.getInstance(DebugWebSocketServer.class);

    private static Server server;
    private static boolean running = false;

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

    public static void stopWebSocketServer() {
        if (server != null && running) {
            server.stop();
        }
    }

    public static boolean isRunning() {
        return running;
    }

}
