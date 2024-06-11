package de.code14.edupydebugger;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class EduPyProjectManagerListener implements ProjectManagerListener {

    @Override
    public void projectClosing(@NotNull Project project) {
        try {
            DebugWebSocketServer.stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
