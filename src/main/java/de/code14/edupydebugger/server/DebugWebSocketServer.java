package de.code14.edupydebugger.server;

import com.intellij.openapi.diagnostic.Logger;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.tyrus.server.Server;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

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
        httpServer = HttpServer.createSimpleServer("/", 8081);

        // StaticHttpHandler for handling the Web Application
        StaticHttpHandler staticHttpHandler;
        try {
            Path resourceBasePath = Paths.get(Objects.requireNonNull(DebugWebSocketServer.class.getResource("/static")).toURI());
            staticHttpHandler = new StaticHttpHandler(resourceBasePath.toString());
        } catch (FileSystemNotFoundException | URISyntaxException e) {
            // Fall back to loading from the classpath if the resource is inside a JAR
            staticHttpHandler = new StaticHttpHandler("/static/");
        }
        httpServer.getServerConfiguration().addHttpHandler(staticHttpHandler, "/");

        // Start Tyrus Websocket server on the same port under the /websockets context
        websocketServer = new Server("localhost", 8082, "/websockets", null, DebugWebSocketServer.class);
        websocketServer.start();

        httpServer.start();
        isRunning = true;
        //LOG.info("Server started on port 8080");
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
