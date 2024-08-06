package de.code14.edupydebugger;

//import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.glassfish.tyrus.container.grizzly.server.GrizzlyServerContainer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class EduPyProjectManagerListener implements ProjectManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EduPyProjectManagerListener.class);


    private static ToolWindow currentDebugToolWindow;


    @Override
    public void projectClosing(@NotNull Project project) {
        SwingUtilities.invokeLater(() -> {
            // Stop the Websocket Server
            if (DebugWebSocketServer.isRunning()) {
                try {
                    DebugWebSocketServer.stopWebSocketServer();
                    LOGGER.info("Stopped debug web socket server");
                } catch (final Exception e) {
                    LOGGER.error("Failed to stop the websocket server", e);
                }
            }

            // Stop the HTTP Webserver
            if (DebugWebServer.isRunning()) {
                try {
                    DebugWebServer.stopWebServer();
                    LOGGER.info("Stopped debug web server");
                } catch (final Exception e) {
                    LOGGER.error("Failed to start the http server", e);
                }
            }

            DebuggerToolWindowFactory.closeJBCefBrowser();
        });
    }

    public static void setCurrentDebugToolWindow(ToolWindow currentDebugToolWindow) {
        EduPyProjectManagerListener.currentDebugToolWindow = currentDebugToolWindow;
    }


}
