package de.code14.edupydebugger.server;


import com.intellij.openapi.diagnostic.Logger;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author julian
 * @version 1.0
 * @since 19.06.24
 */
@WebListener
@ServerEndpoint(value = "/debug")
public class DebugServerEndpoint {

    private static final Logger LOGGER = Logger.getInstance(DebugServerEndpoint.class);

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private static volatile boolean isConnected = false;

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.info("Opened websocket session " + session.getId());
        isConnected = true;

        // Send any queued messages
        while (!messageQueue.isEmpty()) {
            try {
                String message = messageQueue.take();
                sendDebugInfo(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Thread was interrupted while sending queued messages", e);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("Closed websocket session " + session.getId());
        if (sessions.isEmpty()) {
            isConnected = false;
        }
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        LOGGER.info("Received websocket message " + message);
        try {
            //PlantUMLDiagramGenerator.generateDiagram(message, "diagram.png");
            String output = PlantUMLDiagramGenerator.generateDiagramAsBase64(message);
            System.out.println(output);
            sendDebugInfo(output);
        } catch (IOException e) {
            LOGGER.error("Could not generate diagram", e);
        }
        return message;
    }

    public static void sendDebugInfo(String message) {
        if (!isConnected) {
            LOGGER.warn("Websocket is not connected. Queueing message " + message);
            messageQueue.offer(message);
            return;
        }
        synchronized (sessions) {
            for (Session session : sessions) {
                try {
                    session.getBasicRemote().sendText(message);
                    LOGGER.info("Debug info sent to " + session.getId());
                } catch (IOException e) {
                    LOGGER.error("Error while sending debug info to " + session.getId(), e);
                }
            }
        }
    }

    public static synchronized boolean isConnected() {
        return isConnected;
    }
}
