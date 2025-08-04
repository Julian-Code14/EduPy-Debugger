package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.Objects;

/**
 * The {@code ConsoleController} class is responsible for managing the input interaction with
 * the debugged process. It allows sending user input directly to the process's standard input (stdin),
 * facilitating scenarios such as when a Python program running under debug requests user input via
 * the {@code input()} function.
 * <p>
 * This class uses an {@link ProcessHandler} to interface with the running process and send data to
 * its input stream.
 * </p>
 */
public class ConsoleController {

    private static final Logger LOGGER = Logger.getInstance(ConsoleController.class);

    private ProcessHandler processHandler;

    /**
     * Sets the {@link ProcessHandler} that this controller will use to interact with the process.
     * The {@code ProcessHandler} manages the lifecycle of the process and provides access to the
     * input stream for sending data.
     *
     * @param processHandler the {@code ProcessHandler} associated with the debugged process
     */
    public void setProcessHandler(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    /**
     * Returns the {@link ProcessHandler} instance currently associated with this controller.
     * The {@code ProcessHandler} is responsible for managing the debugged process and its I/O streams.
     *
     * @return the {@code ProcessHandler} instance, or {@code null} if none is set
     */
    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    /**
     * Sends the provided input string to the debugged process's input stream (stdin).
     * <p>
     * This method appends a newline character ({@code \n}) to the input before writing it to the process's
     * input stream, simulating the behavior of typical user input in a terminal environment.
     * </p>
     * <p>
     * If the {@link ProcessHandler} is not set or if there is any issue with writing to the input stream,
     * an {@link IOException} will be thrown.
     * </p>
     *
     * @param input the string to be sent to the process's input stream
     * @throws IOException if an I/O error occurs while sending input to the process
     * @throws NullPointerException if the {@code ProcessHandler}'s input stream is {@code null}
     */
    public void sendInputToProcess(String input) throws IOException {
        // Write the input to the process's stdin, appending a newline character
        Objects.requireNonNull(processHandler.getProcessInput()).write((input + "\n").getBytes());
        processHandler.getProcessInput().flush();

        LOGGER.info("User input was sent to the Process Handler");
    }
}
