package de.code14.edupydebugger.services;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author julian
 * @version 1.0
 * @since 11.03.24
 */
@Service(Service.Level.PROJECT)
public final class MyProjectService {

    public MyProjectService(@NotNull Project project) {
        Logger.getInstance(this.getClass()).info("EduPyDebugger");
        Logger.getInstance(this.getClass()).warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.");
    }

    public int getRandomNumber() {
        return (int) (Math.random() * 100 + 1);
    }

}
