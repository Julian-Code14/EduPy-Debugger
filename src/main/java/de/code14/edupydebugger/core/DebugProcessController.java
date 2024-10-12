package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;

/**
 * The DebugProcessController class provides control over the Python debugging process.
 * It allows the user to resume, pause, step over, step into, and step out of the debugging process.
 * This class interacts with the PyDebugProcess to perform these actions.
 *
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebugProcessController {

    private static final Logger LOGGER = Logger.getInstance(DebugProcessController.class);
    private PyDebugProcess debugProcess;

    /**
     * Sets the PyDebugProcess instance that this controller will manage.
     *
     * @param debugProcess the PyDebugProcess instance to be controlled
     */
    public void setDebugProcess(PyDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    /**
     * Resumes the debugging session if a PyDebugProcess is set.
     * Logs an info message indicating that the debugging process has been resumed.
     */
    public void resume() {
        if (debugProcess != null) {
            debugProcess.getSession().resume();
            LOGGER.info("Resumed debug process");
        }
    }

    /**
     * Pauses the debugging session if a PyDebugProcess is set.
     * Logs an info message indicating that the debugging process has been paused.
     */
    public void pause() {
        if (debugProcess != null) {
            debugProcess.getSession().pause();
            LOGGER.info("Paused debug process");
        }
    }

    /**
     * Steps over the current line of code in the debugging session if a PyDebugProcess is set.
     * Logs an info message indicating that the debugger has stepped over the current line.
     */
    public void stepOver() {
        if (debugProcess != null) {
            debugProcess.getSession().stepOver(false);
            LOGGER.info("Stepped over in debug process");
        }
    }

    /**
     * Steps into the current line of code in the debugging session if a PyDebugProcess is set.
     * Logs an info message indicating that the debugger has stepped into the current line.
     */
    public void stepInto() {
        if (debugProcess != null) {
            debugProcess.getSession().stepInto();
            LOGGER.info("Stepped into in debug process");
        }
    }

    /**
     * Steps out of the current method in the debugging session if a PyDebugProcess is set.
     * Logs an info message indicating that the debugger has stepped out of the current method.
     */
    public void stepOut() {
        if (debugProcess != null) {
            debugProcess.getSession().stepOut();
            LOGGER.info("Stepped out in debug process");
        }
    }

    /**
     * Retrieves the currently set PyDebugProcess instance.
     *
     * @return the PyDebugProcess instance being controlled, or null if none is set
     */
    public PyDebugProcess getDebugProcess() {
        return debugProcess;
    }

}
