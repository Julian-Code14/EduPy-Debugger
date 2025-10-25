package de.code14.edupydebugger.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.ConsoleController;
import de.code14.edupydebugger.core.DebugProcessController;
import de.code14.edupydebugger.core.DebugSessionController;
import de.code14.edupydebugger.server.dto.*;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * WebSocket endpoint that exchanges JSON messages with the frontend.
 * <p>
 * The endpoint receives and sends messages of the form {@code DebugMessage<T>} where
 * {@code type} identifies the channel and {@code payload} carries the data.
 * It maintains a small, last-known-state cache to quickly serve {@code get} queries
 * after a reconnect.
 *
 * <h2>Supported inbound message types</h2>
 * <ul>
 *   <li><b>{@code action}</b> — Payload {@code {"command":"resume|pause|step-over|step-into|step-out"}}:
 *       invokes the respective debugger action via {@link DebugProcessController}.</li>
 *   <li><b>{@code console_input}</b> — Payload {@code {"text":"..."} }:
 *       writes input to the debugged process' console via {@link ConsoleController}.</li>
 *   <li><b>{@code thread_selected}</b> — Payload {@code {"name":"Thread-1"}} (empty or missing → no selection):
 *       stores the selected thread and triggers dynamic analysis in
 *       {@link DebugSessionController}.</li>
 *   <li><b>{@code get}</b> — Payload {@code {"resource":"variables|object_cards|class_diagram|object_diagram|callstack|threads"}}:
 *       immediately re-sends the last cached payload for the requested resource (if any).</li>
 * </ul>
 *
 * <h2>Published outbound message types</h2>
 * <ul>
 *   <li>{@code class_diagram} → {@link DiagramPayload}</li>
 *   <li>{@code object_cards} → {@link ObjectCardPayload}</li>
 *   <li>{@code object_diagram} → {@link DiagramPayload}</li>
 *   <li>{@code variables} → {@link VariablesPayload}</li>
 *   <li>{@code callstack} → {@link CallstackPayload}</li>
 *   <li>{@code threads} → {@link ThreadsPayload}</li>
 *   <li>{@code console} → {@link ConsolePayload}</li>
 * </ul>
 *
 * <p>
 * The endpoint path is {@code /debug}. Session management is thread-safe; outbound messages are
 * broadcast to all connected sessions. If no session is connected, messages are queued (FIFO) and
 * flushed on the next connection.
 */
@WebListener
@ServerEndpoint(value = "/debug")
public class DebugServerEndpoint {

    private static final Logger LOGGER = Logger.getInstance(DebugServerEndpoint.class);
    private static final Gson GSON = new Gson();

    /** Active websocket sessions (thread-safe). */
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    /** FIFO queue for outbound JSON when no client is connected. */
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    /** Controllers for debugger integration and console IO. */
    private static final DebugProcessController debugProcessController = new DebugProcessController();
    private static final ConsoleController consoleController = new ConsoleController();
    private static final DebugSessionController debugSessionController = new DebugSessionController();

    /** True while at least one session is connected. */
    private static volatile boolean isConnected = false;

    // --- Last-known payloads for quick GET responses ---
    private static DiagramPayload    lastClassDiagram;
    private static ObjectCardPayload lastObjectCards;
    private static DiagramPayload    lastObjectDiagram;
    private static VariablesPayload  lastVariables;
    private static CallstackPayload  lastCallstack;
    private static ThreadsPayload    lastThreads;

    /** Currently selected thread name (null if no explicit selection). */
    private static String selectedThread;

    // ======================================================================
    // Lifecycle
    // ======================================================================

    /**
     * Called when a new websocket session is opened. Marks the endpoint as connected and
     * flushes any queued outbound messages to the newly established sessions.
     *
     * @param session the newly opened {@link Session}
     */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.info("Opened websocket session " + session.getId());
        isConnected = true;

        // Flush queued messages in FIFO order
        while (!messageQueue.isEmpty()) {
            try {
                String json = messageQueue.take();
                sendRaw(json);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while sending queued messages", e);
            }
        }
    }

    /**
     * Called when a websocket session closes. Updates connection state when the last
     * session disconnects.
     *
     * @param session the closing {@link Session}
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.info("Closed websocket session " + session.getId());
        if (sessions.isEmpty()) {
            isConnected = false;
        }
    }

    // ======================================================================
    // Inbound handling
    // ======================================================================

    /**
     * Dispatches all inbound JSON messages by {@code type}. Invalid or non-JSON messages are ignored.
     *
     * @param message the raw text frame
     * @param session the sender session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        LOGGER.info("WS recv: " + message);
        DebugMessage<?> msg;
        try {
            msg = GSON.fromJson(message, DebugMessage.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.warn("Non-JSON message ignored: " + message);
            return;
        }
        if (msg == null || msg.type == null) {
            LOGGER.warn("Invalid message: " + message);
            return;
        }

        switch (msg.type) {
            case "action": {
                // payload: { "command": "resume|pause|step-over|step-into|step-out" }
                Map<?, ?> p = (Map<?, ?>) msg.payload;
                String command = String.valueOf(p.get("command"));
                handleAction(command);
                break;
            }
            case "console_input": {
                // payload: { "text": "..." }
                ConsolePayload p = GSON.fromJson(GSON.toJson(msg.payload), ConsolePayload.class);
                if (p != null && p.text != null) {
                    try {
                        consoleController.sendInputToProcess(p.text);
                    } catch (IOException e) {
                        LOGGER.error("Error sending console input", e);
                    }
                }
                break;
            }
            case "thread_selected": {
                // payload: { "name": "Thread-1" } | empty -> null
                Map<?, ?> p = (Map<?, ?>) msg.payload;
                String name = (p == null) ? null : (String) p.get("name");
                selectedThread = (name == null || name.isBlank()) ? null : name;
                try {
                    debugSessionController.performDynamicAnalysis(selectedThread);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case "get": {
                // payload: { "resource": "variables|object_cards|class_diagram|object_diagram|callstack|threads" }
                Map<?, ?> p = (Map<?, ?>) msg.payload;
                String res = String.valueOf(p.get("resource"));
                sendLatest(res);
                break;
            }
            default:
                LOGGER.warn("Unknown message type: " + msg.type);
        }
    }

    /**
     * Translates high-level action commands into debugger operations.
     *
     * @param action one of {@code resume|pause|step-over|step-into|step-out}
     */
    private void handleAction(String action) {
        if (action == null) return;
        switch (action) {
            case "resume":     debugProcessController.resume(); break;
            case "pause":      debugProcessController.pause(); break;
            case "step-over":  debugProcessController.stepOver(); break;
            case "step-into":  debugProcessController.stepInto(); break;
            case "step-out":   debugProcessController.stepOut(); break;
            default: LOGGER.warn("Unknown action: " + action);
        }
    }

    /**
     * Sends the last cached payload for the requested resource, if available.
     *
     * @param resource one of {@code class_diagram|object_cards|object_diagram|variables|callstack|threads}
     */
    private void sendLatest(String resource) {
        switch (resource) {
            case "class_diagram" -> {
                if (lastClassDiagram != null) sendDebugMessage("class_diagram", lastClassDiagram);
            }
            case "object_cards" -> {
                if (lastObjectCards != null) sendDebugMessage("object_cards", lastObjectCards);
            }
            case "object_diagram" -> {
                if (lastObjectDiagram != null) sendDebugMessage("object_diagram", lastObjectDiagram);
            }
            case "variables" -> {
                if (lastVariables != null) sendDebugMessage("variables", lastVariables);
            }
            case "callstack" -> {
                if (lastCallstack != null) sendDebugMessage("callstack", lastCallstack);
            }
            case "threads" -> {
                if (lastThreads != null) sendDebugMessage("threads", lastThreads);
            }
            default -> LOGGER.warn("Unknown get resource: " + resource);
        }
    }

    // ======================================================================
    // Outbound core
    // ======================================================================

    /**
     * Broadcasts a typed JSON message to all connected clients.
     * <p>
     * If no client is connected, the message is queued and will be sent once a session opens.
     *
     * @param type    the message type (e.g., {@code variables}, {@code callstack})
     * @param payload the DTO payload
     */
    public static void sendDebugMessage(String type, Object payload) {
        DebugMessage<Object> m = new DebugMessage<>(type, payload);
        String json = new Gson().toJson(m); // keep consistent with original behavior
        if (!isConnected) {
            messageQueue.offer(json);
            return;
        }
        sendRaw(json);
    }

    /**
     * Low-level sender that writes a pre-serialized JSON string to all sessions.
     *
     * @param json serialized JSON to send
     */
    private static void sendRaw(String json) {
        synchronized (sessions) {
            for (Session s : sessions) {
                try {
                    s.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    LOGGER.error("WS send failed to " + s.getId(), e);
                }
            }
        }
    }

    // ======================================================================
    // Publish helpers (called by other components)
    // ======================================================================

    /**
     * Stores and publishes the latest class diagram (Base64-encoded SVG).
     *
     * @param svgBase64 Base64-encoded SVG data
     */
    public static void publishClassDiagram(String svgBase64) {
        lastClassDiagram = new DiagramPayload();
        lastClassDiagram.svgBase64 = svgBase64;
        sendDebugMessage("class_diagram", lastClassDiagram);
    }

    /**
     * Stores and publishes the latest object cards payload.
     *
     * @param payload {@link ObjectCardPayload}
     */
    public static void publishObjectCards(ObjectCardPayload payload) {
        lastObjectCards = payload;
        sendDebugMessage("object_cards", lastObjectCards);
    }

    /**
     * Stores and publishes the latest object diagram (Base64-encoded SVG).
     *
     * @param svgBase64 Base64-encoded SVG data
     */
    public static void publishObjectDiagram(String svgBase64) {
        lastObjectDiagram = new DiagramPayload();
        lastObjectDiagram.svgBase64 = svgBase64;
        sendDebugMessage("object_diagram", lastObjectDiagram);
    }

    /**
     * Stores and publishes the latest variables payload.
     *
     * @param payload {@link VariablesPayload}
     */
    public static void publishVariables(VariablesPayload payload) {
        lastVariables = payload;
        sendDebugMessage("variables", lastVariables);
    }

    /**
     * Stores and publishes the latest call stack payload.
     *
     * @param payload {@link CallstackPayload}
     */
    public static void publishCallstack(CallstackPayload payload) {
        lastCallstack = payload;
        sendDebugMessage("callstack", lastCallstack);
    }

    /**
     * Stores and publishes the latest threads payload.
     *
     * @param payload {@link ThreadsPayload}
     */
    public static void publishThreads(ThreadsPayload payload) {
        lastThreads = payload;
        sendDebugMessage("threads", lastThreads);
    }

    // ======================================================================
    // Integration setters
    // ======================================================================

    /**
     * Injects the current {@link PyDebugProcess} into the controllers.
     *
     * @param debugProcess the active Python debug process
     */
    public static void setDebugProcess(PyDebugProcess debugProcess) {
        debugProcessController.setDebugProcess(debugProcess);
        debugSessionController.setDebugProcess(debugProcess);
    }

    /**
     * Injects the current {@link ProcessHandler} for console IO.
     *
     * @param processHandler the handler used to write to the debugged process
     */
    public static void setProcessHandler(ProcessHandler processHandler) {
        consoleController.setProcessHandler(processHandler);
    }

    // ======================================================================
    // Accessors
    // ======================================================================

    /**
     * @return {@code true} if at least one websocket session is currently connected.
     */
    public static synchronized boolean isConnected() {
        return isConnected;
    }

    /**
     * @return the shared {@link DebugSessionController} instance.
     */
    public static DebugSessionController getDebugSessionController() {
        return debugSessionController;
    }

    /**
     * @return the currently selected thread name, or {@code null} if none is selected.
     */
    public static String getSelectedThread() {
        return selectedThread;
    }
}