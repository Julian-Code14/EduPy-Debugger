package de.code14.edupydebugger.server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.console.ConsoleController;
import de.code14.edupydebugger.core.console.ConsoleOutputListener;
import de.code14.edupydebugger.core.repl.ReplManager;
import de.code14.edupydebugger.core.DebugProcessController;
import de.code14.edupydebugger.core.DebugSessionController;
import de.code14.edupydebugger.server.dto.*;
import jakarta.servlet.annotation.WebListener;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import de.code14.edupydebugger.server.validation.DebugMessageValidator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Added imports for REPL class diagram generation
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import de.code14.edupydebugger.analysis.staticanalysis.PythonAnalyzer;
import de.code14.edupydebugger.diagram.ClassDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;

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

    /** Tracks which process handlers already have a console listener wired to avoid duplicates. */
    private static final Set<ProcessHandler> wiredHandlers = Collections.synchronizedSet(new HashSet<>());

    // --- Last-known payloads for quick GET responses ---
    private static DiagramPayload    lastClassDiagram;
    private static ObjectCardPayload lastObjectCards;
    private static DiagramPayload    lastObjectDiagram;
    private static VariablesPayload  lastVariables;
    private static CallstackPayload  lastCallstack;
    private static ThreadsPayload    lastThreads;

    /** Currently selected thread name (null if no explicit selection). */
    private static String selectedThread;

    /** Test seam: supplier for the IDE project used in REPL mode static analysis. */
    private static java.util.function.Supplier<Project> projectSupplier = () -> {
        try {
            Project[] open = ProjectManager.getInstance().getOpenProjects();
            return (open != null && open.length > 0) ? open[0] : null;
        } catch (Throwable t) {
            LOGGER.warn("Could not obtain open project for REPL class diagram", t);
            return null;
        }
    };

    /** Test seam: supplier for ClassDiagramParser construction. */
    private static java.util.function.Supplier<ClassDiagramParser> classDiagramParserSupplier =
            () -> new ClassDiagramParser(new PythonAnalyzer());

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

        // Optional: send a connection banner when -Dedupy.ws.banner=true
        if (Boolean.getBoolean("edupy.ws.banner")) {
            try {
                ConsolePayload p = new ConsolePayload();
                p.text = "[EduPy] WebSocket connected: " + session.getId();
                sendDebugMessage("console", p);
            } catch (Throwable ignore) {}
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
     * Dispatches all inbound JSON messages by {@code type}.
     * <p>
     * Parsing and light validation is delegated to {@code server.validation.DebugMessageValidator}
     * to keep this method focused on behavior. Invalid or non‑JSON messages are ignored.
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
                String command = DebugMessageValidator
                        .extractActionCommand(msg.payload, GSON)
                        .orElse(null);
                handleAction(command);
                break;
            }
            case "console_input": {
                // payload: { "text": "..." }
                ConsolePayload p = DebugMessageValidator
                        .extractConsoleInput(msg.payload, GSON)
                        .orElse(null);
                if (p != null && p.text != null) {
                    try {
                        ensureConsoleTarget();
                        consoleController.sendInputToProcess(p.text);
                        // In REPL mode, trigger a variables snapshot after each input
                        if (debugProcessController.getDebugProcess() == null) {
                            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                            try { ReplManager.getInstance().requestSnapshot(); } catch (Exception ignore) {}
                        }
                    } catch (IOException e) {
                        LOGGER.error("Error sending console input", e);
                    }
                }
                break;
            }
            case "thread_selected": {
                // payload: { "name": "Thread-1" } | empty -> null
                selectedThread = DebugMessageValidator
                        .extractSelectedThread(msg.payload, GSON);
                try {
                    debugSessionController.performDynamicAnalysis(selectedThread);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case "get": {
                // payload: { "resource": "variables|object_cards|class_diagram|object_diagram|callstack|threads" }
                DebugMessageValidator
                        .extractGetResource(msg.payload, GSON)
                        .ifPresent(this::sendLatest);
                break;
            }
            case "repl_reset": {
                // stop REPL process and clear controller state
                try {
                    ReplManager.getInstance().stopRepl();
                } catch (Throwable ignore) {}
                consoleController.setProcessHandler(null);
                // clear cached payloads
                lastClassDiagram = null;
                lastObjectCards  = null;
                lastObjectDiagram= null;
                lastVariables    = null;
                lastCallstack    = null;
                lastThreads      = null;
                break;
            }
            default:
                LOGGER.warn("Unknown message type: " + msg.type);
        }
    }

    /**
     * Ensures that console input has a valid target. If no debug process is present,
     * a lightweight Python REPL is started and its output is bridged back to the UI.
     */
    private static void ensureConsoleTarget() {
        ProcessHandler current = consoleController.getProcessHandler();
        if (current != null && !current.isProcessTerminated()) return;

        try {
            ProcessHandler repl = ReplManager.getInstance().ensureReplStarted();
            consoleController.setProcessHandler(repl);
            // Wire output once
            synchronized (wiredHandlers) {
                if (!wiredHandlers.contains(repl)) {
                    new ConsoleOutputListener(repl).attachConsoleListeners();
                    wiredHandlers.add(repl);
                }
            }
            // Clear call stack when switching to REPL to avoid stale frames from previous debug sessions
            try {
                CallstackPayload empty = new CallstackPayload();
                empty.frames = java.util.Collections.emptyList();
                publishCallstack(empty);
            } catch (Throwable ignore) {}
        } catch (Exception ex) {
            LOGGER.warn("Failed to start fallback REPL", ex);
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
                if (lastClassDiagram != null) {
                    sendDebugMessage("class_diagram", lastClassDiagram);
                } else {
                    // In REPL mode (no debug process), lazily generate the class diagram from sources
                    if (debugProcessController.getDebugProcess() == null) {
                        try {
                            generateAndPublishClassDiagramForOpenProject();
                        } catch (Exception e) {
                            LOGGER.warn("Failed to generate class diagram in REPL mode", e);
                        }
                    }
                }
            }
            case "object_cards" -> {
                if (lastObjectCards != null) sendDebugMessage("object_cards", lastObjectCards);
            }
            case "object_diagram" -> {
                if (lastObjectDiagram != null) sendDebugMessage("object_diagram", lastObjectDiagram);
            }
            case "variables" -> {
                if (lastVariables != null) {
                    sendDebugMessage("variables", lastVariables);
                } else {
                    // In REPL mode, ensure we have a target and trigger a snapshot
                    if (debugProcessController.getDebugProcess() == null) {
                        try {
                            ensureConsoleTarget();
                            // Delay lightly to avoid printing [] on fresh REPL
                            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                            ReplManager.getInstance().requestSnapshot();
                        } catch (Exception e) {
                            LOGGER.warn("Failed to request REPL snapshot on GET", e);
                        }
                    }
                }
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
        String json = GSON.toJson(m);
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

    // ======================================================================
    // Helpers
    // ======================================================================

    /**
     * Generates and publishes a class diagram for the first open IDE project.
     * No‑op if no project is open or generation fails.
     */
    private static void generateAndPublishClassDiagramForOpenProject() throws java.io.IOException {
        Project project = projectSupplier.get();
        if (project == null) return;
        ClassDiagramParser parser = classDiagramParserSupplier.get();
        String plantUml;
        try {
            var app = com.intellij.openapi.application.ApplicationManager.getApplication();
            if (app != null) {
                plantUml = com.intellij.openapi.application.ReadAction.compute(() -> parser.generateClassDiagram(project));
            } else {
                plantUml = parser.generateClassDiagram(project);
            }
        } catch (Throwable t) {
            // Fallback for non‑IDE test contexts without Application instance
            plantUml = parser.generateClassDiagram(project);
        }
        String base64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUml);
        publishClassDiagram(base64);
    }

    // Visible for tests
    static void setProjectSupplier(java.util.function.Supplier<Project> supplier) {
        if (supplier != null) {
            projectSupplier = supplier;
        } else {
            projectSupplier = () -> {
                try {
                    Project[] open = ProjectManager.getInstance().getOpenProjects();
                    return (open != null && open.length > 0) ? open[0] : null;
                } catch (Throwable t) {
                    LOGGER.warn("Could not obtain open project for REPL class diagram", t);
                    return null;
                }
            };
        }
    }

    // Visible for tests
    static void setClassDiagramParserSupplier(java.util.function.Supplier<ClassDiagramParser> supplier) {
        if (supplier != null) {
            classDiagramParserSupplier = supplier;
        } else {
            classDiagramParserSupplier = () -> new ClassDiagramParser(new PythonAnalyzer());
        }
    }
}
