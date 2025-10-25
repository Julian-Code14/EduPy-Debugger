package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import org.jetbrains.annotations.NotNull;

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
                LOGGER.info("Console Output: " + text);
                ConsolePayload payload = new ConsolePayload();
                payload.text = text;
                DebugServerEndpoint.sendDebugMessage("console", payload);
            }
        });
    }
}