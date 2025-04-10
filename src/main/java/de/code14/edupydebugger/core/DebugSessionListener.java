package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.staticanalysis.PythonAnalyzer;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.diagram.ClassDiagramParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
/**
 * The DebugSessionListener class listens for changes in the debug session's stack frame.
 * It triggers both static and dynamic code analysis when the stack frame changes during a debugging session.
 * The results of these analyses are sent to the client via WebSocket for visualization.
 * <p>
 * This listener is particularly focused on Python debugging sessions within IntelliJ-based IDEs.
 * It interacts with a custom WebSocket server to provide real-time updates to a web-based debugging interface.
 * <p>
 * The class diagram and object diagram generated during the analysis are encoded as PlantUML diagrams.
 *
 * @author julian
 * @version 0.3.0
 * @since 0.1.0
 */
public class DebugSessionListener implements XDebugSessionListener {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    private final XDebugProcess debugProcess;
    private final XDebugSession session;

    private final ClassDiagramParser classDiagramParser;

    private static final String THREADS_PREFIX = "threads:";

    /**
     * Constructs a DebugSessionListener for the given debug process.
     * Initializes the session and sets up the class diagram parser.
     *
     * @param debugProcess the XDebugProcess representing the current debug session
     */
    public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        this.session = debugProcess.getSession();

        this.classDiagramParser = new ClassDiagramParser(new PythonAnalyzer());

        try {
            performStaticAnalysis((PyDebugProcess) debugProcess);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Invoked when the active stack frame in the debug session changes. Logs a message indicating that dynamic analysis
     * is about to start, and then proceeds with dynamic analysis steps if the debug process is a
     * Python debug process ({@link PyDebugProcess}).
     * <p>
     * If no specific thread is currently selected in {@link DebugServerEndpoint}, it generates a list of available
     * threads for the user to pick from. Afterwards, it calls the dynamic analysis method ({@link DebugSessionController#performDynamicAnalysis(String)}) using
     * the selected thread name.
     * <p>
     * Any {@link IOException} encountered during the analysis is rethrown as a {@link RuntimeException}.
     */
    @Override
    public void stackFrameChanged() {
        LOGGER.info("Stack frame changed, initiating dynamic analysis.");

        if (debugProcess instanceof PyDebugProcess pyDebugProcess) {
            try {
                generateThreadOptions();
                DebugServerEndpoint.getDebugSessionController().performDynamicAnalysis(DebugServerEndpoint.getSelectedThread());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Generates a semicolon-separated list of all Python threads in the current debug session, including their names
     * and states. For each thread, this method appends a state label such as <em>(läuft...)</em> or <em>(angehalten)</em>
     * depending on whether the thread is running, suspended, or killed.
     *
     * <p>Once constructed, the thread options string is:
     * <ul>
     *   <li>Stored in the {@link DebugServerEndpoint} via {@link DebugServerEndpoint#setThreadOptionsString(String)}</li>
     *   <li>Transmitted to the client with a <em>THREADS_PREFIX</em> so the client can populate a thread selection UI.</li>
     * </ul>
     */
    private void generateThreadOptions() {
        List<PyThreadInfo> pyThreadInfos = DebuggerUtils.getThreads(session);
        StringBuilder threadsString = new StringBuilder();
        for (PyThreadInfo pyThreadInfo : pyThreadInfos) {
            threadsString.append(pyThreadInfo.getName());
            if (pyThreadInfo.getState() == null) {
                threadsString.append(" (wartend...)");
            } else {
                switch (pyThreadInfo.getState()) {
                    case RUNNING:
                        threadsString.append(" (läuft...)");
                        break;
                    case SUSPENDED:
                        threadsString.append(" (angehalten)");
                        break;
                    case KILLED:
                        threadsString.append(" (beendet)");
                        break;
                }
            }
            threadsString.append(";");
        }

        DebugServerEndpoint.setThreadOptionsString(threadsString.toString());
        DebugServerEndpoint.sendDebugInfo(THREADS_PREFIX + threadsString);
    }

    /**
     * Performs static code analysis and updates the class diagram.
     * This method generates a class diagram in PlantUML format and sends it to the WebSocket server.
     *
     * @param pyDebugProcess the Python debug process
     */
    protected void performStaticAnalysis(PyDebugProcess pyDebugProcess) throws IOException {
        String classDiagramPlantUmlString = classDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
        generateAndUpdateClassDiagramInServerEndpoint(classDiagramPlantUmlString);
    }

    /**
     * Generates a PlantUML class diagram and updates it in the debug server endpoint for get requests.
     *
     * @param classDiagramPlantUmlString the PlantUML string to generate the class diagram from
     */
    private void generateAndUpdateClassDiagramInServerEndpoint(String classDiagramPlantUmlString) throws IOException {
        String base64Diagram = PlantUMLDiagramGenerator.generateDiagramAsBase64(classDiagramPlantUmlString);
        DebugServerEndpoint.setClassDiagramPlantUmlImage(base64Diagram);
    }

}
