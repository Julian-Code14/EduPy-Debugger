package de.code14.edupydebugger.server;

import com.intellij.openapi.diagnostic.Logger;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.tyrus.server.Server;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
@ServerEndpoint("/debug")
public class DebugWebSocketServer {

    private static final Logger LOG = Logger.getInstance(DebugWebSocketServer.class);

    private static HttpServer httpServer;
    private static Server websocketServer;
    private static boolean isRunning = false;

    @OnOpen
    public void onOpen(Session session) {
        LOG.info(String.format("Websocket session with id \"%s\" opened.", session.getId()));
        // Handle onOpen logic
    }

    @OnClose
    public void onClose(Session session) {
        LOG.info(String.format("Websocket session with id \"%s\" closed.", session.getId()));
        // Handle onClose logic
    }

    @OnMessage
    public String handleMessage(String message) {
        LOG.info(String.format("New websocket message received: \"%s\"", message));
        // Handle incoming messages from the client
        return "Received: " + message;
    }

    public static void startServer() throws IOException, DeploymentException, URISyntaxException {
        if (isRunning) {
            LOG.info("Server is already running");
            return;
        }

        // Start Grizzly HTTP server on port 8080
        httpServer = new HttpServer();
        NetworkListener listener = new NetworkListener("EduPyDebuggerUI", "localhost", 8081);
        httpServer.addListener(listener);

        // StaticHttpHandler for handling the Web Application
        httpServer.getServerConfiguration().addHttpHandler(new org.glassfish.grizzly.http.server.CLStaticHttpHandler(
                DebugWebSocketServer.class.getClassLoader(), "/static/"
        ), "/");

        // Start Tyrus Websocket server on another open port under the /websockets context
        Map<String, Object> properties = new HashMap<>();
        websocketServer = new Server("localhost", 8082, "", properties, DebugWebSocketServer.class);

        try {
            httpServer.start();
            websocketServer.start();
            isRunning = true;
            LOG.info("Servers started successfully on ports 8081 (HTTP) and 8082 (WebSocket)");
        } catch (Exception e) {
            LOG.error("Failed to start servers", e);
            stopServer();
        }

    }

    public static void stopServer() throws IOException {
        if (!isRunning) {
            LOG.info("Server is not running");
            return;
        }

        // Stop the WebSocket server
        if (websocketServer != null) {
            websocketServer.stop();
            LOG.info("Websocket Server stopped");
        }

        // Stop the HTTP server
        if (httpServer != null) {
            httpServer.shutdownNow();
            LOG.info("HTTP Server stopped");
        }

        isRunning = false;
    }

    public static boolean isRunning() {
        return isRunning;
    }

}
