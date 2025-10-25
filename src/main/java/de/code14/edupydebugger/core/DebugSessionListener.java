package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.staticanalysis.PythonAnalyzer;
import de.code14.edupydebugger.diagram.ClassDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ThreadDTO;
import de.code14.edupydebugger.server.dto.ThreadsPayload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Listens to PyCharm/XDebugger session events and keeps the frontend in sync with the current
 * debugging state by publishing diagrams and thread information via {@link DebugServerEndpoint}.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li><b>Static analysis on session start:</b> Generates a class diagram (PlantUML → Base64 SVG)
 *       using {@link ClassDiagramParser} and {@link PythonAnalyzer} and publishes it to the client.</li>
 *   <li><b>Dynamic updates on frame changes:</b> On each {@link #stackFrameChanged()} event, publishes
 *       the current set of threads and triggers a dynamic analysis run in
 *       {@link de.code14.edupydebugger.core.DebugSessionController} for the selected thread.</li>
 * </ul>
 * <p>
 * This listener is Python-focused and expects the {@link XDebugProcess} to be a {@link PyDebugProcess}.
 */
public class DebugSessionListener implements XDebugSessionListener {

    private static final Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    /** The active XDebugger process (expected to be a {@link PyDebugProcess}). */
    private final XDebugProcess debugProcess;

    /** Parses project sources to produce a PlantUML class diagram for the frontend. */
    private final ClassDiagramParser classDiagramParser;

    /**
     * Creates a new session listener and immediately performs a one-time static analysis pass.
     *
     * @param debugProcess the current debugger process; must be an instance of {@link PyDebugProcess}
     */
    public DebugSessionListener(XDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        this.classDiagramParser = new ClassDiagramParser(new PythonAnalyzer());

        try {
            performStaticAnalysis((PyDebugProcess) debugProcess);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Invoked when the active stack frame changes (e.g., stepping, pause/resume).
     * <p>
     * Publishes the current thread list to the frontend and triggers a dynamic analysis run for the
     * currently selected thread (if any), which in turn updates variables, call stack, and object
     * diagrams through the {@link DebugServerEndpoint}.
     */
    @Override
    public void stackFrameChanged() {
        LOGGER.info("Stack frame changed -> dynamic analysis");

        if (debugProcess instanceof PyDebugProcess py) {
            try {
                publishThreads(py);
                DebugServerEndpoint.getDebugSessionController().performDynamicAnalysis(DebugServerEndpoint.getSelectedThread());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Collects all debugger threads from the current session and publishes them as a JSON payload.
     * <p>
     * Each {@link ThreadDTO} includes the thread name and a normalized state string. If the underlying
     * state is {@code null}, it defaults to {@code "WAITING"}.
     *
     * @param py the active Python debug process
     */
    private void publishThreads(PyDebugProcess py) {
        List<PyThreadInfo> infos = DebuggerUtils.getThreads(py.getSession());
        ThreadsPayload payload = new ThreadsPayload();
        payload.threads = new ArrayList<>();
        for (PyThreadInfo ti : infos) {
            ThreadDTO t = new ThreadDTO();
            t.name = ti.getName();
            t.state = (ti.getState() == null) ? "WAITING" : ti.getState().name();
            payload.threads.add(t);
        }
        DebugServerEndpoint.publishThreads(payload);
    }

    /**
     * Performs a one-time static project analysis to generate the class diagram and publishes it
     * as a Base64-encoded SVG via the {@link DebugServerEndpoint}.
     *
     * @param py the active Python debug process (used to access the IntelliJ project)
     * @throws IOException if PlantUML diagram generation or encoding fails
     */
    protected void performStaticAnalysis(PyDebugProcess py) throws IOException {
        String plantUml = classDiagramParser.generateClassDiagram(py.getProject());
        String base64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUml);
        DebugServerEndpoint.publishClassDiagram(base64);
    }
}