package de.code14.edupydebugger.core.console;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.Objects;

/**
 * Manages sending input to the target process' stdin.
 */
public class ConsoleController {

    private static final Logger LOGGER = Logger.getInstance(ConsoleController.class);

    private ProcessHandler processHandler;

    public void setProcessHandler(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public ProcessHandler getProcessHandler() { return processHandler; }

    public void sendInputToProcess(String input) throws IOException {
        Objects.requireNonNull(processHandler.getProcessInput()).write((input + "\n").getBytes());
        processHandler.getProcessInput().flush();
        LOGGER.info("User input was sent to the Process Handler");
    }
}

