package de.code14.edupydebugger.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.AnimatedIcon;
import com.intellij.util.concurrency.AppExecutorUtil;
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

    // Simple card-based UI to show a loading screen on first start (local to factory instance)
    private static final String CARD_LOADING = "loading";
    private static final String CARD_BROWSER = "browser";


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
        // Root with CardLayout
        CardLayout cards = new CardLayout();
        JPanel root = new JPanel(cards);

        // Loading panel with spinner and message
        JPanel loading = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(8,8,4,8);
        JLabel title = new JLabel("Starting EduPy-Debugger…");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 1f));
        loading.add(title, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0,8,8,8);
        JLabel subtitle = new JLabel("Launching local servers and UI…");
        loading.add(subtitle, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(8,8,8,8);
        JLabel spinner = new JLabel(new AnimatedIcon.Default());
        loading.add(spinner, gbc);

        // Browser container (we plug JBCef when ready)
        JPanel browserContainer = new JPanel(new BorderLayout());

        root.add(loading, CARD_LOADING);
        root.add(browserContainer, CARD_BROWSER);
        cards.show(root, CARD_LOADING);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(root, "", false);
        toolWindow.getContentManager().addContent(content);

        // Start servers asynchronously and then show the browser when ready
        initializeBrowser();
        waitForServersThenShowBrowser(browserContainer, cards, root);
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
        // Start servers asynchronously to avoid UI stalls; browser loads regardless
        final DebugWebSocketServer ws = DebugWebSocketServer.getInstance();
        final DebugWebServer http = DebugWebServer.getInstance();
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                if (!ws.isRunning()) {
                    ws.startWebSocketServer();
                }
                if (!http.isRunning()) {
                    http.startWebServer();
                }
            } catch (Throwable t) {
                LOGGER.warn("Could not start web/socket servers from ToolWindow init", t);
            }
        });

        // Defer actual JBCef creation to when servers are (likely) up; see waitForServersThenShowBrowser
    }

    private void waitForServersThenShowBrowser(JPanel browserContainer, CardLayout cards, JPanel root) {
        final DebugWebSocketServer ws = DebugWebSocketServer.getInstance();
        final DebugWebServer http = DebugWebServer.getInstance();
        // Use a scheduled check instead of busy-wait sleep
        java.util.concurrent.ScheduledExecutorService ses = AppExecutorUtil.getAppScheduledExecutorService();
        final long deadline = System.currentTimeMillis() + 5000; // up to 5s
        final Runnable tryShow = new Runnable() {
            @Override public void run() {
                boolean ready = ws.isRunning() && http.isRunning();
                boolean timeout = System.currentTimeMillis() >= deadline;
                if (!ready && !timeout) {
                    ses.schedule(this, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    if (jbCefBrowser == null && JBCefApp.isSupported()) {
                        jbCefBrowser = new JBCefBrowser("http://127.0.0.1:8026/index.html");
                        LOGGER.info("Loading JBCef browser...");
                    } else if (jbCefBrowser != null) {
                        jbCefBrowser.loadURL("http://127.0.0.1:8026/index.html");
                        LOGGER.info("Reloaded JBCef browser");
                    } else {
                        LOGGER.error("JBCefApp is not supported");
                    }
                    if (jbCefBrowser != null) {
                        browserContainer.removeAll();
                        browserContainer.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);
                    }
                    cards.show(root, CARD_BROWSER);
                });
            }
        };
        ses.schedule(tryShow, 0, java.util.concurrent.TimeUnit.MILLISECONDS);
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
