package de.code14.edupydebugger.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class DebuggerToolWindowFactory implements ToolWindowFactory {


    @Override
    public boolean isApplicable(@NotNull Project project) {
        return ToolWindowFactory.super.isApplicable(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JBCefBrowser jbCefBrowser = new JBCefBrowser("https://online.visual-paradigm.com/drive/#diagramlist:proj=0&dashboard");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ToolWindowFactory.super.init(toolWindow);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return ToolWindowFactory.super.shouldBeAvailable(project);
    }

    @SuppressWarnings("removal")
    @Override
    public boolean isDoNotActivateOnStart() {
        return ToolWindowFactory.super.isDoNotActivateOnStart();
    }

    @Override
    public @Nullable ToolWindowAnchor getAnchor() {
        return ToolWindowFactory.super.getAnchor();
    }

    @Override
    public @Nullable Icon getIcon() {
        return ToolWindowFactory.super.getIcon();
    }

    public void openToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
        if (toolWindow != null) {
            toolWindow.show();
        } else {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            toolWindow = toolWindowManager.registerToolWindow("DebuggerToolWindow", true, ToolWindowAnchor.BOTTOM);
            this.createToolWindowContent(project, toolWindow);
            toolWindow.show();
        }
    }
}
