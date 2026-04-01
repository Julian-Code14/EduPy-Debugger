package de.code14.edupydebugger.core;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a lightweight fallback Python REPL (outside of a debug session).
 * If the user types into the console without an active debug process, we
 * start a plain Python interpreter and wire its IO into the existing console
 * bridge so the UX behaves like a minimal REPL.
 */
public class ReplManager {

    private static final Logger LOGGER = Logger.getInstance(ReplManager.class);
    private static final ReplManager INSTANCE = new ReplManager();

    private OSProcessHandler replHandler;

    public static ReplManager getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures a REPL process is running and returns its ProcessHandler.
     * Tries "python3" first, then "python".
     */
    public synchronized ProcessHandler ensureReplStarted() throws Exception {
        if (replHandler != null && !replHandler.isProcessTerminated()) {
            return replHandler;
        }

        List<String> cmd = new ArrayList<>();
        String executable = "python3";
        try {
            // prefer python3 if available
            executable = "python3";
            cmd.add(executable);
            cmd.add("-i"); // interactive
            cmd.add("-q"); // quiet banner
            GeneralCommandLine gcl = new GeneralCommandLine(cmd);
            gcl.withEnvironment("PYTHONUNBUFFERED", "1");
            replHandler = new OSProcessHandler(gcl);
            LOGGER.info("Started fallback REPL using '" + executable + "'");
        } catch (Throwable primary) {
            // retry with python
            cmd.clear();
            executable = "python";
            cmd.add(executable);
            cmd.add("-i");
            cmd.add("-q");
            GeneralCommandLine gcl2 = new GeneralCommandLine(cmd);
            gcl2.withEnvironment("PYTHONUNBUFFERED", "1");
            replHandler = new OSProcessHandler(gcl2);
            LOGGER.info("Started fallback REPL using '" + executable + "'");
        }
        return replHandler;
    }

    /** Stops the REPL if it is running. */
    public synchronized void stopRepl() {
        if (replHandler != null) {
            try {
                replHandler.destroyProcess();
            } catch (Throwable t) {
                // ignore; best-effort
            } finally {
                replHandler = null;
            }
        }
    }
}

