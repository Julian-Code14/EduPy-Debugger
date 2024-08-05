package de.code14.edupydebugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import jakarta.websocket.DeploymentException;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author julian
 * @version 1.0
 * @since 10.06.24
 */
public class DebugProcessListener implements XDebuggerManagerListener {

    private static final Logger LOGGER = Logger.getInstance(DebugProcessListener.class);

    private final Project project;
    private final DebuggerToolWindowFactory debuggerToolWindowFactory;

    public DebugProcessListener(Project project) {
        this.project = project;
        this.debuggerToolWindowFactory = new DebuggerToolWindowFactory();
    }

    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        final XDebugSession debugSession = debugProcess.getSession();

        LOGGER.info("Debug Process started");

        // Start the Websocket Server
        if (!DebugWebSocketServer.isRunning()) {
            try {
                DebugWebSocketServer.startWebSocketServer();
            } catch (final Exception e) {
                LOGGER.error("Failed to start the websocket server", e);
            }
        }

        // Start the HTTP Webserver
        if (!DebugWebServer.isRunning()) {
            try {
                DebugWebServer.startWebServer();
            } catch (final Exception e) {
                LOGGER.error("Failed to start the http server", e);
            }
        }

        DebugServerEndpoint.setDebugProcess(debugProcess);

        SwingUtilities.invokeLater(() -> {
            // Hide the default Debug Tool Window content
            ToolWindow defaultDebugToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG);
            if (defaultDebugToolWindow != null) {
                defaultDebugToolWindow.setAvailable(false);
            }

            // Open the Tool Window
            debuggerToolWindowFactory.openToolWindow(project);
        });

        // Register DebugSessionListener
        debugSession.addSessionListener(new DebugSessionListener(debugProcess));
    }

    @Override
    public void processStopped(@NotNull XDebugProcess debugProcess) {
        SwingUtilities.invokeLater(() -> {
            // Stop the Websocket Server
            if (DebugWebSocketServer.isRunning()) {
                try {
                    DebugWebSocketServer.stopWebSocketServer();
                } catch (final Exception e) {
                    LOGGER.error("Failed to stop the websocket server", e);
                }
            }

            // Stop the HTTP Webserver
            if (DebugWebServer.isRunning()) {
                try {
                    DebugWebServer.stopWebServer();
                } catch (final Exception e) {
                    LOGGER.error("Failed to start the http server", e);
                }
            }

            // Close Debugger Tool Window
            ToolWindow currentDebugToolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
            if (currentDebugToolWindow != null) {
                currentDebugToolWindow.hide();
            }
        });
    }

}
