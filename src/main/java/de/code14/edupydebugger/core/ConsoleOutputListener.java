package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import de.code14.edupydebugger.server.dto.VariablesPayload;
import de.code14.edupydebugger.server.dto.VariableDTO;
import de.code14.edupydebugger.server.dto.ValueDTO;
import de.code14.edupydebugger.server.dto.ObjectCardPayload;
import de.code14.edupydebugger.server.dto.CardDTO;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Listens to the standard output and error streams of the debugged Python process
 * and forwards all console output to the frontend via a JSON-based WebSocket message.
 * <p>
 * Each line of text emitted by the {@link ProcessHandler} is wrapped in a
 * {@link ConsolePayload} object and sent to the connected WebSocket clients through
 * the {@link DebugServerEndpoint} using the message type {@code "console"}.
 * <p>
 * This allows the IDE-integrated debugger to mirror the live console output
 * in the custom EduPy Debugger UI.
 */
public class ConsoleOutputListener {

    private static final Logger LOGGER = Logger.getInstance(ConsoleOutputListener.class);
    private final ProcessHandler processHandler;
    private boolean startupLineSuppressed = false;
    private static final String SNAPSHOT_PREFIX = "__EDUPY_SNAPSHOT__";
    private final StringBuilder snapshotBuffer = new StringBuilder();
    private boolean capturingSnapshot = false;

    /**
     * Constructs a new listener bound to the given {@link ProcessHandler}.
     *
     * @param processHandler the process handler whose console output should be observed
     */
    public ConsoleOutputListener(@NotNull ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    /**
     * Attaches a {@link ProcessListener} to the process handler to observe text output events.
     * <p>
     * Whenever new text becomes available, this listener logs the output locally
     * and sends it to all connected WebSocket clients as a JSON message of type
     * {@code "console"}.
     */
    public void attachConsoleListeners() {
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                if (shouldSuppressStartupLine(text)) {
                    // Drop the noisy launcher command printed by the Python debugger once.
                    startupLineSuppressed = true;
                    LOGGER.info("Suppressed debugger startup line");
                    return;
                }

                // REPL snapshot handling across partial chunks
                if (text != null) {
                    if (capturingSnapshot) {
                        snapshotBuffer.append(text);
                        if (flushSnapshotIfComplete()) return; // handled as variables payload
                    } else {
                        int idx = text.indexOf(SNAPSHOT_PREFIX);
                        if (idx >= 0) {
                            capturingSnapshot = true;
                            snapshotBuffer.setLength(0);
                            snapshotBuffer.append(text.substring(idx));
                            if (flushSnapshotIfComplete()) return;
                            // keep buffering until complete
                            return;
                        }
                    }
                }

                LOGGER.info("Console Output: " + text);
                ConsolePayload payload = new ConsolePayload();
                payload.text = text;
                DebugServerEndpoint.sendDebugMessage("console", payload);
            }
        });
    }

    /**
     * Returns true for the initial pydevd launcher command that PyCharm prints into the console, e.g.
     * "/path/to/python .../pydev/pydevd.py --multiprocess --client 127.0.0.1 --port 51817 --file main.py".
     * We suppress this once per process to keep the console clean.
     */
    private boolean shouldSuppressStartupLine(String text) {
        if (startupLineSuppressed || text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;

        // Heuristics: contains pydevd.py and typical debug args
        boolean containsPydevd = t.contains("pydev/pydevd.py") || t.contains("pydevd.py");
        boolean hasArg = (t.contains("--client") || t.contains("--port")) && t.contains("--file");
        // starts with a python executable path is common, but not required for the check
        return containsPydevd && hasArg;
    }

    /** Parses REPL JSON snapshot and publishes Variables/ObjectCards/ObjectDiagram. */
    private void publishVariablesFromSnapshot(String json) throws java.io.IOException {
        com.google.gson.Gson g = new com.google.gson.Gson();
        Object parsed = g.fromJson(json, Object.class);

        java.util.List<?> varsList = null;
        java.util.Map<?,?> objectsMap = null;

        if (parsed instanceof java.util.List) {
            varsList = (java.util.List<?>) parsed; // backward compatibility
        } else if (parsed instanceof java.util.Map) {
            java.util.Map<?,?> root = (java.util.Map<?,?>) parsed;
            Object v = root.get("variables");
            if (v instanceof java.util.List) varsList = (java.util.List<?>) v;
            Object o = root.get("objects");
            if (o instanceof java.util.Map) objectsMap = (java.util.Map<?,?>) o;
        }

        // Build objects first (for composite attribute snippet)
        java.util.Map<String, ObjectInfo> objects = new java.util.HashMap<>();
        if (objectsMap != null && !objectsMap.isEmpty()) {
            for (java.util.Map.Entry<?,?> e : objectsMap.entrySet()) {
                String id = String.valueOf(e.getKey());
                Object val = e.getValue();
                if (!(val instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> om = (java.util.Map<String, Object>) val;
                String ref = String.valueOf(om.get("ref"));
                java.util.List<AttributeInfo> attrs = new java.util.ArrayList<>();
                Object attrsVal = om.get("attrs");
                if (attrsVal instanceof java.util.List) {
                    for (Object a : (java.util.List<?>) attrsVal) {
                        if (!(a instanceof java.util.Map)) continue;
                        @SuppressWarnings("unchecked")
                        java.util.Map<String,Object> am = (java.util.Map<String,Object>) a;
                        String an = String.valueOf(am.get("name"));
                        String at = String.valueOf(am.get("type"));
                        String av = String.valueOf(am.get("value"));
                        attrs.add(new AttributeInfo(an, at, av, "public"));
                    }
                }
                java.util.List<String> refs = new java.util.ArrayList<>();
                refs.add(ref);
                objects.put(id, new ObjectInfo(refs, attrs));
            }
        }

        // Variables payload (now that objects are available)
        java.util.List<VariableDTO> varDtos = new java.util.ArrayList<>();
        if (varsList != null) {
            for (Object o : varsList) {
                if (!(o instanceof java.util.Map)) continue;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> m = (java.util.Map<String, Object>) o;
                VariableDTO dto = new VariableDTO();
                dto.id = String.valueOf(m.get("id"));
                dto.names = java.util.Collections.singletonList(String.valueOf(m.get("name")));
                dto.pyType = String.valueOf(m.get("type"));
                dto.scope = String.valueOf(m.getOrDefault("scope", "global"));
                ValueDTO val = new ValueDTO();
                val.repr = String.valueOf(m.get("repr"));
                dto.value = val;
                varDtos.add(dto);
            }
            PayloadPublisher.publishVariablesWithSnippet(varDtos, objects);
        }

        // Object cards + diagram
        if (!objects.isEmpty()) {
            PayloadPublisher.publishObjects(objects);
        }
    }

    /** Flushes buffer when a full snapshot line is received. */
    private boolean flushSnapshotIfComplete() {
        int nl = snapshotBuffer.indexOf("\n");
        if (nl < 0) return false;
        String line = snapshotBuffer.substring(0, nl);
        capturingSnapshot = false;
        try {
            if (line.startsWith(SNAPSHOT_PREFIX)) {
                publishVariablesFromSnapshot(line.substring(SNAPSHOT_PREFIX.length()));
                return true;
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to handle REPL snapshot line", t);
        }
        return false;
    }
}
