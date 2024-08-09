package de.code14.edupydebugger.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author julian
 * @version 1.0
 * @since 11.06.24
 */
public class DebuggerToolWindowFactory implements ToolWindowFactory {

    private final static Logger LOGGER = Logger.getInstance(DebuggerToolWindowFactory.class);

    private static JBCefBrowser jbCefBrowser;


    @Override
    public boolean isApplicable(@NotNull Project project) {
        return ToolWindowFactory.super.isApplicable(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initializeBrowser();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    // Is called before createToolWindowContent
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

    private void initializeBrowser() {
        if (jbCefBrowser == null && JBCefApp.isSupported()) {
            jbCefBrowser = new JBCefBrowser("http://localhost:8026/index.html");
            LOGGER.info("Loading JBCef browser...");
        } else if (jbCefBrowser != null) {
            jbCefBrowser.loadURL("http://localhost:8026/index.html");
            LOGGER.info("Reloaded JBCef browser");
        } else {
            LOGGER.error("JBCefApp is not supported");
        }
    }

    public void openToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    public void closeToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
        if (toolWindow != null) {
            toolWindow.hide();
        }
    }

    public static void closeJBCefBrowser() {
        if (jbCefBrowser != null) {
            jbCefBrowser.dispose();
            jbCefBrowser.getCefBrowser().close(true);
            jbCefBrowser = null;
        }
    }

    public static void openPythonTutor() {
        jbCefBrowser.loadURL("https://pythontutor.com/python-compiler.html#mode=edit");
    }

    public static void reloadEduPyDebugger() {
        jbCefBrowser.loadURL("http://localhost:8026/index.html");
    }
}
