package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code ConsoleOutputListener} class is responsible for listening to the console output
 * (both stdout and stderr) from a process being debugged and forwarding that output for further
 * processing, such as sending it to a WebSocket.
 * <p>
 * This class is primarily used to capture output from a process being debugged and log it,
 * as well as send the captured output to another system, such as a WebSocket endpoint.
 * This can be useful for monitoring, debugging, or providing feedback to users in real-time.
 * </p>
 *
 * @author julian
 * @version 0.2.0
 * @since 0.2.0
 */
public class ConsoleOutputListener {

    private static final Logger LOGGER = Logger.getInstance(ConsoleOutputListener.class);

    private static final String CONSOLE_PREFIX = "console:";

    private final ProcessHandler processHandler;

    public ConsoleOutputListener(@NotNull ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    /**
     * Attaches listeners to the {@link ProcessHandler} to capture stdout and stderr from the debugged process.
     * <p>
     * The captured output is forwarded to the WebSocket for further processing.
     * </p>
     *
     * @throws IllegalStateException if the {@code processHandler} is {@code null}
     */
    public void attachConsoleListeners() {
        if (processHandler == null) {
            throw new IllegalStateException("ProcessHandler cannot be null when attaching console listeners");
        }

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                String text = event.getText();
                LOGGER.info("Console Output: " + text);

                sendToWebSocket(text);
            }
        });
    }

    /**
     * Simulates sending the console output to a WebSocket or another component.
     *
     * @param text the console output to be sent
     */
    private void sendToWebSocket(String text) {
        // Example logic to send console output to WebSocket or another system
        DebugServerEndpoint.sendDebugInfo(CONSOLE_PREFIX + text);
    }

}
