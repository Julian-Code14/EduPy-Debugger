package de.code14.edupydebugger;

//import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import jakarta.websocket.DeploymentException;
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

    //private static final Logger LOG = Logger.getInstance(DebugProcessListener.class);

    private final Project project;

    public DebugProcessListener(Project project) {
        this.project = project;
    }

    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        final XDebugSession debugSession = debugProcess.getSession();

        SwingUtilities.invokeLater(() -> {
            if (!DebugWebSocketServer.isRunning()) {
                try {
                    DebugWebSocketServer.startServer();
                } catch (IOException | DeploymentException | URISyntaxException e) {
                    //LOG.error("Failed to start the server", e);
                }
            }

            // Hide the default Debug Tool Window content
            ToolWindow defaultDebugToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG);
            if (defaultDebugToolWindow != null) {
                defaultDebugToolWindow.setAvailable(false);
            }

            // Open the Tool Window
            DebuggerToolWindowFactory debuggerToolWindowFactory = new DebuggerToolWindowFactory();
            debuggerToolWindowFactory.openToolWindow(project);
        });
    }

    @Override
    public void processStopped(@NotNull XDebugProcess debugProcess) {
        // Optional: Tool-Fenster wieder schließen
    }

}
