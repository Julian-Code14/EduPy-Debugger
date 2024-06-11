package de.code14.edupydebugger.server;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.tyrus.server.Server;

import java.io.IOException;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
@ServerEndpoint("/debug")
public class DebugWebSocketServer {

    private static HttpServer httpServer;
    private static Server websocketServer;
    private static boolean isRunning = false;

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        // Handle incoming messages from the client
        session.getBasicRemote().sendText("Received: " + message);
    }

    public static void startServer() throws IOException, DeploymentException {
        if (isRunning) {
            System.out.println("Server is already running");
            return;
        }

        // Start Grizzly HTTP server on port 8080
        httpServer = HttpServer.createSimpleServer("/", 8080);
        // Start Tyrus Websocket server on the same port under the /websockets context
        websocketServer = new Server("localhost", 8080, "/websockets", null, DebugWebSocketServer.class);
        websocketServer.start();

        httpServer.start();
        System.out.println("Server started on port 8080");
    }

    public static void stopServer() throws IOException {
        if (!isRunning) {
            System.out.println("Server is not running");
            return;
        }

        // Stop the WebSocket server
        if (websocketServer != null) {
            websocketServer.stop();
            System.out.println("Websocket Server stopped");
        }

        // Stop the HTTP server
        if (httpServer != null) {
            httpServer.shutdownNow();
            System.out.println("HTTP Server stopped");
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

}
