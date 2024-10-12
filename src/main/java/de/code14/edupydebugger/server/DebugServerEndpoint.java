package de.code14.edupydebugger.server;


import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.ConsoleController;
import de.code14.edupydebugger.core.DebugProcessController;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.Session;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebSocket server endpoint for handling debugger-related communications.
 * This class manages WebSocket connections, processes incoming messages, and sends debug information
 * such as diagrams and variable states to connected clients.
 * <p>
 * The server also queues messages if no client is currently connected, ensuring that messages are not lost.
 * It also provides control over the debugging process through the WebSocket interface.
 * </p>
 *
 * @author julian
 * @version 0.2.0
 * @since 0.1.0
 */
@WebListener
@ServerEndpoint(value = "/debug")
public class DebugServerEndpoint {

    private static final Logger LOGGER = Logger.getInstance(DebugServerEndpoint.class);

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private static final DebugProcessController debugProcessController = new DebugProcessController();
    private static final ConsoleController consoleController = new ConsoleController();

    private static volatile boolean isConnected = false;

    // Debug content to be shared with clients
    private static String classDiagramPlantUmlImage;
    private static String variablesString;
    private static String objectCardPlantUmlImagesData;
    private static String objectDiagramPlantUmlImage;

    private static final Map<String, Runnable> actionMap = new HashMap<>();
    static {
        actionMap.put("resume", debugProcessController::resume);
        actionMap.put("pause", debugProcessController::pause);
        actionMap.put("step-over", debugProcessController::stepOver);
        actionMap.put("step-into", debugProcessController::stepInto);
        actionMap.put("step-out", debugProcessController::stepOut);
    }

    /**
     * Called when a new WebSocket connection is opened.
     *
     * @param session the WebSocket session that was opened
     */
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

    /**
     * Called when a WebSocket connection is closed.
     *
     * @param session the WebSocket session that was closed
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("Closed websocket session " + session.getId());
        if (sessions.isEmpty()) {
            isConnected = false;
        }
    }

    /**
     * Called when a message is received from a client.
     *
     * @param message the message received
     * @param session the WebSocket session that sent the message
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("Received WebSocket message " + message);
        if (message.startsWith("action:")) {
            handleActionMessage(message.substring("action:".length()));
        } else if (message.startsWith("get:")) {
            handleGetMessage(message.substring("get:".length()));
        } else if (message.startsWith("navigate:")) {
            DebuggerToolWindowFactory.openPythonTutor();
        } else if (message.startsWith("Success:")) {
            LOGGER.debug("Action was successfully handled by client.");
        } else {
            LOGGER.warn("Unknown message received: " + message);
        }
    }

    /**
     * Sends debug information to all connected WebSocket clients.
     *
     * @param message the debug information to send
     */
    public static void sendDebugInfo(String message) {
        if (!isConnected) {
            LOGGER.warn("Websocket is not connected. Queueing message " + message);
            boolean offer = messageQueue.offer(message);
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

    /**
     * Handles action messages received from the client.
     * The actions include controlling the debugger (resume, pause, step-over, step-into, step-out).
     *
     * @param action the action command received
     */
    private void handleActionMessage(String action) {
        if (action.startsWith("console-input:")) {
            // Extract the actual input string after "console-input:"
            String consoleInput = action.substring("console-input:".length());

            // Forward the input to ConsoleController
            try {
                consoleController.sendInputToProcess(consoleInput);
            } catch (IOException e) {
                LOGGER.error("Error sending input to the process: " + consoleInput, e);
            }
        } else {
            Runnable command = actionMap.get(action);
            if (command != null) {
                command.run();
            } else {
                LOGGER.warn("Unknown action received: " + action);
            }
        }
    }

    /**
     * Handles get requests received from the client.
     * The get requests include fetching the class diagram, object cards diagram, object diagram, and variables.
     *
     * @param request the get request received
     */
    private void handleGetMessage(String request) {
        switch (request) {
            case "cd":
                sendDebugInfo(classDiagramPlantUmlImage);
                break;
            case "oc":
                sendDebugInfo(objectCardPlantUmlImagesData);
                break;
            case "od":
                sendDebugInfo("od:" + objectDiagramPlantUmlImage);
                break;
            case "variables":
                sendDebugInfo("variables:" + variablesString);
                break;
            default:
                LOGGER.warn("Unknown get request received: " + request);
                break;
        }
    }

    /**
     * Checks whether there is an active WebSocket connection.
     *
     * @return true if connected, false otherwise
     */
    public static synchronized boolean isConnected() {
        return isConnected;
    }

    /**
     * Sets the current debug process for controlling the debugger.
     *
     * @param debugProcess the PyDebugProcess to control
     */
    public static void setDebugProcess(PyDebugProcess debugProcess) {
        debugProcessController.setDebugProcess(debugProcess);
    }

    /**
     * Sets the current process handler for controlling the console.
     *
     * @param processHandler the ProcessHandler to control the console
     */
    public static void setProcessHandler(ProcessHandler processHandler) {
        consoleController.setProcessHandler(processHandler);
    }

    /**
     * Sets the PlantUML image for the class diagram.
     *
     * @param base64PlantUml the Base64 encoded PlantUML image
     */
    public static void setClassDiagramPlantUmlImage(String base64PlantUml) {
        classDiagramPlantUmlImage = base64PlantUml;
    }

    /**
     * Sets the string representation of variables.
     *
     * @param variablesString the string representing the variables
     */
    public static void setVariablesString(String variablesString) {
        DebugServerEndpoint.variablesString = variablesString;
    }

    /**
     * Sets the PlantUML image for the object cards diagram.
     *
     * @param base64PlantUmlData the Base64 encoded PlantUML image
     */
    public static void setObjectCardPlantUmlImagesData(String base64PlantUmlData) {
        objectCardPlantUmlImagesData = base64PlantUmlData;
    }

    /**
     * Sets the PlantUML image for the object diagram.
     *
     * @param base64PlantUml the Base64 encoded PlantUML image
     */
    public static void setObjectDiagramPlantUmlImage(String base64PlantUml) {
        objectDiagramPlantUmlImage = base64PlantUml;
    }

}
