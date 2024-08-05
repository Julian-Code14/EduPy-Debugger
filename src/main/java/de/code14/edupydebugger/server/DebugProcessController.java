package de.code14.edupydebugger.server;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;

/**
 * @author julian
 * @version 1.0
 * @since 05.08.24
 */
public class DebugProcessController {

    private static final Logger LOGGER = Logger.getInstance(DebugProcessController.class);
    private XDebugProcess debugProcess;

    public void setDebugProcess(XDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    public void resume() {
        if (debugProcess != null) {
            debugProcess.getSession().resume();
            LOGGER.info("Resumed debug process");
        }
    }

    public void pause() {
        if (debugProcess != null) {
            debugProcess.getSession().pause();
            LOGGER.info("Paused debug process");
        }
    }

    public void stepOver() {
        if (debugProcess != null) {
            debugProcess.getSession().stepOver(false);
            LOGGER.info("Stepped over in debug process");
        }
    }

    public void stepInto() {
        if (debugProcess != null) {
            debugProcess.getSession().stepInto();
            LOGGER.info("Stepped into in debug process");
        }
    }

    public void stepOut() {
        if (debugProcess != null) {
            debugProcess.getSession().stepOut();
            LOGGER.info("Stepped out in debug process");
        }
    }

}
