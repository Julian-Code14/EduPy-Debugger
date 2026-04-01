package de.code14.edupydebugger.core.console;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.core.publish.PayloadPublisher;
import de.code14.edupydebugger.core.snapshot.NormalizedSnapshot;
import de.code14.edupydebugger.core.snapshot.ReplSnapshotAdapter;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import org.jetbrains.annotations.NotNull;

/**
 * Forwards console output and parses REPL snapshots to publish variables/objects.
 */
public class ConsoleOutputListener {

    private static final Logger LOGGER = Logger.getInstance(ConsoleOutputListener.class);
    private final ProcessHandler processHandler;
    private boolean startupLineSuppressed = false;
    private static final String SNAPSHOT_PREFIX = "__EDUPY_SNAPSHOT__";
    private final StringBuilder snapshotBuffer = new StringBuilder();
    private boolean capturingSnapshot = false;

    public ConsoleOutputListener(@NotNull ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public void attachConsoleListeners() {
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                if (shouldSuppressStartupLine(text)) {
                    startupLineSuppressed = true;
                    LOGGER.info("Suppressed debugger startup line");
                    return;
                }

                if (text != null) {
                    if (capturingSnapshot) {
                        snapshotBuffer.append(text);
                        if (flushSnapshotIfComplete()) return;
                    } else {
                        int idx = text.indexOf(SNAPSHOT_PREFIX);
                        if (idx >= 0) {
                            capturingSnapshot = true;
                            snapshotBuffer.setLength(0);
                            snapshotBuffer.append(text.substring(idx));
                            if (flushSnapshotIfComplete()) return;
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

    private boolean shouldSuppressStartupLine(String text) {
        if (startupLineSuppressed || text == null) return false;
        String t = text.trim();
        if (t.isEmpty()) return false;
        boolean containsPydevd = t.contains("pydev/pydevd.py") || t.contains("pydevd.py");
        boolean hasArg = (t.contains("--client") || t.contains("--port")) && t.contains("--file");
        return containsPydevd && hasArg;
    }

    private void publishVariablesFromSnapshot(String json) throws java.io.IOException {
        NormalizedSnapshot snapshot = ReplSnapshotAdapter.fromJson(json);
        PayloadPublisher.publishVariablesWithSnippet(snapshot.variables(), snapshot.objects());
        PayloadPublisher.publishObjects(snapshot.objects());
    }

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

