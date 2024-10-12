package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author julian
 * @version 0.2.0
 * @since 0.1.0
 */
public class DebugProcessListener implements XDebuggerManagerListener {

    private static final Logger LOGGER = Logger.getInstance(DebugProcessListener.class);

    private final Project project;
    private final DebuggerToolWindowFactory debuggerToolWindowFactory;

    /**
     * Constructs a DebugProcessListener for the specified project.
     *
     * @param project the IntelliJ IDEA project associated with this listener
     */
    public DebugProcessListener(Project project) {
        this.project = project;
        this.debuggerToolWindowFactory = new DebuggerToolWindowFactory();
    }

    /**
     * This method is called when the debugging process starts.
     * It initializes the WebSocket and HTTP servers, sets up the debug process endpoint,
     * and opens the custom debugger tool window while hiding the default one.
     *
     * @param debugProcess the XDebugProcess representing the debugging process
     */
    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        final XDebugSession debugSession = debugProcess.getSession();

        LOGGER.info("Debug Process started");

        // Start the WebSocket server if it's not already running
        if (!DebugWebSocketServer.getInstance().isRunning()) {
            try {
                DebugWebSocketServer.getInstance().startWebSocketServer();
            } catch (final Exception e) {
                LOGGER.error("Failed to start the websocket server", e);
            }
        }

        // Start the HTTP server if it's not already running
        if (!DebugWebServer.getInstance().isRunning()) {
            try {
                DebugWebServer.getInstance().startWebServer();
            } catch (final Exception e) {
                LOGGER.error("Failed to start the http server", e);
            }
        }

        // Set the debug process in the WebSocket endpoint
        DebugServerEndpoint.setDebugProcess((PyDebugProcess) debugProcess);

        // Set the process handler in the Websocket endpoint
        DebugServerEndpoint.setProcessHandler(debugProcess.getProcessHandler());

        // Attach ConsoleOutputListener to capture console output
        ConsoleOutputListener consoleOutputListener = this.createConsoleOutputListener(debugProcess.getProcessHandler());
        consoleOutputListener.attachConsoleListeners();

        // Update UI components in the Swing event dispatch thread
        SwingUtilities.invokeLater(() -> {
            // Hide the default Debug Tool Window content
            ToolWindow defaultDebugToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG);
            if (defaultDebugToolWindow != null) {
                defaultDebugToolWindow.setAvailable(false);
            }

            // Open the custom Debugger Tool Window
            debuggerToolWindowFactory.openToolWindow(project);
        });

        // Register DebugSessionListener to monitor stack frame changes during the debugging session
        debugSession.addSessionListener(new DebugSessionListener(debugProcess));
    }

    /**
     * This method is called when the debugging process stops.
     * It reloads the custom debugger UI to ensure that any remaining UI elements are reset.
     *
     * @param debugProcess the XDebugProcess representing the debugging process
     */
    @Override
    public void processStopped(@NotNull XDebugProcess debugProcess) {
        DebuggerToolWindowFactory.reloadEduPyDebugger();
    }

    /**
     * Factory method to create a ConsoleOutputListener. This method can be overridden in tests.
     *
     * @param processHandler the process handler used by the ConsoleOutputListener
     * @return a new instance of ConsoleOutputListener
     */
    protected ConsoleOutputListener createConsoleOutputListener(ProcessHandler processHandler) {
        return new ConsoleOutputListener(processHandler);
    }

}
