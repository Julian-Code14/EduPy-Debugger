package de.code14.edupydebugger;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import de.code14.edupydebugger.ui.DebuggerToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author julian
 * @version 1.0
 * @since 10.06.24
 */
public class DebugProcessListener implements XDebuggerManagerListener {

    private final Project project;

    public DebugProcessListener(Project project) {
        this.project = project;
    }

    @Override
    public void processStarted(@NotNull XDebugProcess debugProcess) {
        final XDebugSession debugSession = debugProcess.getSession();

        // Open the Tool Window
        SwingUtilities.invokeLater(() -> {
            DebuggerToolWindowFactory debuggerToolWindowFactory = new DebuggerToolWindowFactory();
            debuggerToolWindowFactory.openToolWindow(project);
        });
    }

    @Override
    public void processStopped(@NotNull XDebugProcess debugProcess) {
        // Optional: Tool-Fenster wieder schließen
    }

    private void openToolWindow(Project project) {

    }

}
