package de.code14.edupydebugger.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * A factory class for creating and managing the Debugger Tool Window.
 * This class is responsible for initializing the JBCefBrowser, adding it to the tool window,
 * and providing methods to control the visibility and state of the tool window.
 * <p>
 * The tool window is used to display a browser window within the IDE that loads the debugging interface.
 * It leverages the JBCefBrowser to render web content, such as the debug UI hosted locally or on the web.
 */
public class DebuggerToolWindowFactory implements ToolWindowFactory {

    private final static Logger LOGGER = Logger.getInstance(DebuggerToolWindowFactory.class);

    private static JBCefBrowser jbCefBrowser;


    /**
     * Creates the content of the Debugger Tool Window.
     * Initializes the JBCefBrowser and adds it to the tool window panel.
     *
     * @param project the current project
     * @param toolWindow the tool window to which content is added
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        initializeBrowser();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Initializes the tool window before content is created.
     *
     * @param toolWindow the tool window to initialize
     */
    @Override
    public void init(@NotNull ToolWindow toolWindow) {
        ToolWindowFactory.super.init(toolWindow);
    }

    /**
     * Determines if the tool window should be available for the given project.
     *
     * @param project the project to check
     * @return true if the tool window should be available, false otherwise
     */
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return ToolWindowFactory.super.shouldBeAvailable(project);
    }

    /**
     * Initializes the JBCefBrowser if not already initialized.
     * Loads the debugger UI from a specified local URL.
     */
    private void initializeBrowser() {
        if (jbCefBrowser == null && JBCefApp.isSupported()) {
            jbCefBrowser = new JBCefBrowser("http://127.0.0.1:8026/index.html");
            LOGGER.info("Loading JBCef browser...");
        } else if (jbCefBrowser != null) {
            jbCefBrowser.loadURL("http://127.0.0.1:8026/index.html");
            LOGGER.info("Reloaded JBCef browser");
        } else {
            LOGGER.error("JBCefApp is not supported");
        }
    }

    /**
     * Opens the Debugger Tool Window.
     *
     * @param project the current project
     */
    public void openToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    /**
     * Closes the Debugger Tool Window.
     *
     * @param project the current project
     */
    public void closeToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("DebuggerToolWindow");
        if (toolWindow != null) {
            toolWindow.hide();
        }
    }

    /**
     * Disposes the JBCefBrowser when the tool window is no longer needed.
     * This method is typically called when the project is closed.
     */
    public static void closeJBCefBrowser() {
        if (jbCefBrowser != null) {
            jbCefBrowser.dispose();
            jbCefBrowser.getCefBrowser().close(true);
            jbCefBrowser = null;
        }
    }

    /**
     * Opens the Python Tutor website in the JBCefBrowser.
     * This method is used to navigate to the Python Tutor site for debugging or educational purposes.
     */
    public static void openPythonTutor() {
        jbCefBrowser.loadURL("https://pythontutor.com/python-compiler.html#mode=edit");
    }

    /**
     * Reloads the EduPy Debugger interface in the JBCefBrowser.
     * This method is typically used to refresh the content displayed in the tool window.
     */
    public static void reloadEduPyDebugger() {
        jbCefBrowser.loadURL("http://127.0.0.1:8026/index.html");
    }
}
