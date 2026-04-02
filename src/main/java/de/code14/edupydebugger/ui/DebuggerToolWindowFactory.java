package de.code14.edupydebugger.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import de.code14.edupydebugger.server.DebugWebServer;
import de.code14.edupydebugger.server.DebugWebSocketServer;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import de.code14.edupydebugger.core.repl.ReplManager;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.Module;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

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
    private volatile boolean browserAttached = false;


    /**
     * Creates the content of the Debugger Tool Window.
     * Initializes the JBCefBrowser and adds it to the tool window panel.
     *
     * @param project the current project
     * @param toolWindow the tool window to which content is added
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            ReplManager.getInstance().setWorkingDirectory(project.getBasePath());
            // Collect source roots for PYTHONPATH
            VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentSourceRoots();
            java.util.List<String> paths = new java.util.ArrayList<>();
            for (VirtualFile vf : roots) {
                if (vf != null && vf.exists()) paths.add(vf.getPath());
            }
            ReplManager.getInstance().setExtraPythonPaths(paths);
            // Set project interpreter if available
            try {
                Module[] modules = ModuleManager.getInstance(project).getModules();
                for (Module m : modules) {
                    Sdk pySdk = PythonSdkUtil.findPythonSdk(m);
                    if (pySdk != null && pySdk.getHomePath() != null) {
                        ReplManager.getInstance().setInterpreterPath(pySdk.getHomePath());
                        break;
                    }
                }
            } catch (Throwable ignore2) {}
        } catch (Throwable ignore) {}
        // Panel zuerst einhängen, Browser erst anfügen wenn sichtbar (macOS Accessibility Avoidance)
        JPanel panel = new JPanel(new BorderLayout());

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Wenn das Panel „showing“ wird, den Browser anhängen (verhindert getLocationOnScreen‑Fehler)
        panel.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                    SwingUtilities.invokeLater(() -> attachBrowserIfNeeded(panel));
                }
            }
        });
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
        // Ensure servers are running even without an active debug session
        try {
            if (!DebugWebSocketServer.getInstance().isRunning()) {
                DebugWebSocketServer.getInstance().startWebSocketServer();
            }
            if (!DebugWebServer.getInstance().isRunning()) {
                DebugWebServer.getInstance().startWebServer();
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not start web/socket servers from ToolWindow init", t);
        }

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
     * Fügt die Browser‑Komponente nur an, wenn nötig und erst nach dem Anzeigen des Panels.
     */
    private void attachBrowserIfNeeded(JPanel panel) {
        if (browserAttached) return;
        initializeBrowser();
        if (jbCefBrowser != null && jbCefBrowser.getComponent().getParent() != panel) {
            panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
            browserAttached = true;
            panel.revalidate();
            panel.repaint();
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
     * <p>
     * Defensive and side‑effect free: ignores disposal issues so IDE shutdown stays quiet.
     * No‑op when the browser was never initialized.
     */
    public static void closeJBCefBrowser() {
        if (jbCefBrowser != null) {
            try {
                jbCefBrowser.dispose();
                jbCefBrowser.getCefBrowser().close(true);
            } catch (Throwable ignore) {
                // keep quiet to avoid noisy shutdown warnings
            } finally {
                jbCefBrowser = null;
            }
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
     * Reloads the EduPy Debugger interface if the browser exists (no‑op otherwise).
     */
    public static void reloadEduPyDebugger() {
        if (jbCefBrowser != null) {
            jbCefBrowser.loadURL("http://127.0.0.1:8026/index.html");
        }
    }
}
