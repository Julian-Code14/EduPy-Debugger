package de.code14.edupydebugger.toolWindow;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import de.code14.edupydebugger.MyBundle;
import de.code14.edupydebugger.services.MyProjectService;

import javax.swing.*;

import static com.sun.java.accessibility.util.AWTEventMonitor.addActionListener;

/**
 * @author julian
 * @version 1.0
 * @since 11.03.24
 */
public class MyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        MyToolWindow myToolWindow = new MyToolWindow(toolWindow, project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(myToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static class MyToolWindow {

        private final MyProjectService service;

        public MyToolWindow(ToolWindow toolWindow, Project project) {
            // Jetzt wird das Project-Objekt korrekt an MyProjectService übergeben.
            this.service = ServiceManager.getService(project, MyProjectService.class);
        }

        public JBPanel getContent() {
            JBPanel panel = new JBPanel();
            JBLabel label = new JBLabel(MyBundle.message("randomLabel", "?"));

            panel.add(label);
            JButton shuffleButton = new JButton(MyBundle.message("shuffle"));
            shuffleButton.addActionListener(e -> label.setText(MyBundle.message("randomLabel", service.getRandomNumber())));
            panel.add(shuffleButton);
            return panel;
        }
    }
}
