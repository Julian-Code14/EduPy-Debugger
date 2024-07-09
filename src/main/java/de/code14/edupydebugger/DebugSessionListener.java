package de.code14.edupydebugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.frame.XStackFrame;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.jetbrains.annotations.NotNull;

/**
 * @author julian
 * @version 1.0
 * @since 05.07.24
 */
public class DebugSessionListener implements XDebugSessionListener {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    private final XDebugSession session;

    public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
        this.session = debugProcess.getSession();
    }


    @Override
    public void stackFrameChanged() {
        LOGGER.info("stackFrameChanged");
    }

}
