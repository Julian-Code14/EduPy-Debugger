package de.code14.edupydebugger.server;


import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
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
    private static final DebugProcessController debugProcessController = new DebugProcessController();

    // Contents
    private static String classDiagramPlantUmlImage;
    private static String variablesString;
    private static String objectCardsPlantUmlImage;
    private static String objectDiagramPlantUmlImage;

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
    public void onMessage(String message, Session session) {
        LOGGER.info("Received websocket message " + message);
        if (message.startsWith("action:")) {
            switch (message.substring("action:".length())) {
                case "resume":
                    debugProcessController.resume();
                    break;
                case "pause":
                    debugProcessController.pause();
                    break;
                case "step-over":
                    debugProcessController.stepOver();
                    break;
                case "step-into":
                    debugProcessController.stepInto();
                    break;
                case "step-out":
                    debugProcessController.stepOut();
                    break;
                default:
                    LOGGER.warn("Unknown action received: " + message);
                    break;
            }
        } else if (message.startsWith("get:")) {
            switch (message.substring("get:".length())) {
                case "cd":
                    sendDebugInfo(classDiagramPlantUmlImage);
                    break;
                case "oc":
                    sendDebugInfo("oc:" + objectCardsPlantUmlImage);
                    break;
                case "od":
                    sendDebugInfo("od:" + objectDiagramPlantUmlImage);
                    break;
                case "variables":
                    sendDebugInfo("variables:" + variablesString);
                    break;
                default:
                    LOGGER.warn("Unknown get request received: " + message);
                    break;
            }
        } else if (message.startsWith("navigate:")) {
            DebuggerToolWindowFactory.openPythonTutor();
        } else {
            LOGGER.warn("Unknown message received: " + message);
        }
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
                    LOGGER.info("Debug info sent to " + session.getId() + ": " + message);
                } catch (IOException e) {
                    LOGGER.error("Error while sending debug info to " + session.getId(), e);
                }
            }
        }
    }

    public static synchronized boolean isConnected() {
        return isConnected;
    }

    public static void setDebugProcess(PyDebugProcess debugProcess) {
        debugProcessController.setDebugProcess(debugProcess);
    }

    public static void setClassDiagramPlantUmlImage(String base64PlantUml) {
        classDiagramPlantUmlImage = base64PlantUml;
    }

    public static void setVariablesString(String variablesString) {
        DebugServerEndpoint.variablesString = variablesString;
    }

    public static void setObjectCardsPlantUmlImage(String base64PlantUml) {
        objectCardsPlantUmlImage = base64PlantUml;
    }

    public static void setObjectDiagramPlantUmlImage(String base64PlantUml) {
        objectDiagramPlantUmlImage = base64PlantUml;
    }

}
