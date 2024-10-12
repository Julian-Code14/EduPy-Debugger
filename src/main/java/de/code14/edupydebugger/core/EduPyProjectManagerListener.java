package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Listener class that responds to events related to the lifecycle of a project.
 * This class handles the initialization and cleanup of the debug web server and the debug tool window.
 * Specifically, it ensures that the web server and web socket server are stopped when the project is closed.
 *
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class EduPyProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EduPyProjectManagerListener.class);


    /**
     * Called when a project is closing. This method ensures that the debug web server and web socket server are stopped,
     * and the JBCef browser used in the debug tool window is closed.
     *
     * @param project the project that is closing
     */
    @Override
    public void projectClosing(@NotNull Project project) {
        SwingUtilities.invokeLater(() -> {
            // Stop the Websocket Server
            if (DebugWebSocketServer.getInstance().isRunning()) {
                try {
                    DebugWebSocketServer.getInstance().stopWebSocketServer();
                    LOGGER.info("Stopped debug web socket server");
                } catch (final Exception e) {
                    LOGGER.error("Failed to stop the websocket server", e);
                }
            }

            // Stop the HTTP Webserver
            if (DebugWebServer.getInstance().isRunning()) {
                try {
                    DebugWebServer.getInstance().stopWebServer();
                    LOGGER.info("Stopped debug web server");
                } catch (final Exception e) {
                    LOGGER.error("Failed to start the http server", e);
                }
            }

            // Close the JBCefBrowser in the DebuggerToolWindowFactory
            DebuggerToolWindowFactory.closeJBCefBrowser();
        });
    }


}
