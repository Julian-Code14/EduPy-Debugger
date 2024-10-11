package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.jetbrains.annotations.NotNull;

/**
 * @author julian
 * @version 1.0
 * @since 11.10.24
 */
public class ConsoleOutputListener {

    private static final Logger LOGGER = Logger.getInstance(ConsoleOutputListener.class);

    private static final String CONSOLE_PREFIX = "console:";

    private final ProcessHandler processHandler;

    public ConsoleOutputListener(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    /**
     * Attaches listeners to capture stdout and stderr from the debugged process.
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
