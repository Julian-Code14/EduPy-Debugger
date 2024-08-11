package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.analysis.*;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.diagram.ClassDiagramParser;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
 * @version 1.0
 * @since 05.07.24
 */
public class DebugSessionListener implements XDebugSessionListener {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    private static final String VARIABLES_PREFIX = "variables:";
    private static final String OBJECT_CARDS_PREFIX = "oc:";
    private static final String OBJECT_DIAGRAM_PREFIX = "od:";

    private final XDebugProcess debugProcess;
    private final XDebugSession session;

    private final ClassDiagramParser classDiagramParser;


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
    }


    /**
     * Called when the stack frame changes during the debug session.
     * Initiates both dynamic and static code analysis, and sends the results to the WebSocket server.
     */
    @Override
    public void stackFrameChanged() {
        LOGGER.info("Stack frame changed, initiating analysis.");

        if (debugProcess instanceof PyDebugProcess pyDebugProcess) {
            // Perform dynamic analysis
            performDynamicAnalysis();

            // Perform static code analysis
            performStaticAnalysis(pyDebugProcess);
        }
    }

    /**
     * Performs dynamic analysis by analyzing the current stack frames.
     * This includes extracting variables and objects and sending this data to the WebSocket server.
     */
    private void performDynamicAnalysis() {
        List<PyStackFrame> pyStackFrames = DebuggerUtils.getAllStackFrames(this.session);
        StackFrameAnalyzer stackFrameAnalyzer = new StackFrameAnalyzer(pyStackFrames);

        stackFrameAnalyzer.analyzeFrames();

        // Handle variables and objects extracted from the stack frames
        handleVariables(stackFrameAnalyzer);
        handleObjects(stackFrameAnalyzer);
    }

    /**
     * Handles the collection and sending of variable information.
     * Converts the variable data to a string format and sends it to the WebSocket server.
     *
     * @param stackFrameAnalyzer the analyzer responsible for processing stack frames
     */
    private void handleVariables(StackFrameAnalyzer stackFrameAnalyzer) {
        Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();
        StringBuilder variablesString = new StringBuilder();

        // Construct the variables string in the format "key=value;"
        for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
            String values = String.join(",", entry.getValue());
            variablesString.append(entry.getKey()).append("=").append(values).append(";");
        }

        LOGGER.info(variablesString.toString());
        DebugServerEndpoint.setVariablesString(variablesString.toString());
        DebugServerEndpoint.sendDebugInfo(VARIABLES_PREFIX + variablesString);
    }

    /**
     * Handles the collection and sending of object information.
     * Converts the object data into PlantUML diagrams and sends the diagrams to the WebSocket server.
     *
     * @param stackFrameAnalyzer the analyzer responsible for processing stack frames
     */
    private void handleObjects(StackFrameAnalyzer stackFrameAnalyzer) {
        Map<String, ObjectInfo> objects = stackFrameAnalyzer.getObjects();

        // Generate the object cards diagram in PlantUML format
        String objectCardsPlantUmlString = ObjectDiagramParser.generateObjectCards(objects);
        generateAndSendDiagram(objectCardsPlantUmlString, "objectCards");

        // Generate the object relationships diagram in PlantUML format
        String objectDiagramPlantUmlString = ObjectDiagramParser.generateObjectDiagram(objects);
        generateAndSendDiagram(objectDiagramPlantUmlString, "objectDiagram");
    }

    /**
     * Performs static code analysis and updates the class diagram.
     * This method generates a class diagram in PlantUML format and sends it to the WebSocket server.
     *
     * @param pyDebugProcess the Python debug process
     */
    private void performStaticAnalysis(PyDebugProcess pyDebugProcess) {
        String classDiagramPlantUmlString = classDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
        generateAndSendDiagram(classDiagramPlantUmlString, "classDiagram");
    }

    /**
     * Generates a PlantUML diagram and sends it to the debug server endpoint.
     *
     * @param plantUmlString the PlantUML string to generate the diagram from
     * @param type           the type of diagram being generated ("objectCards", "objectDiagram", "classDiagram")
     */
    private void generateAndSendDiagram(String plantUmlString, String type) {
        try {
            String base64Diagram = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlString);
            switch (type) {
                case "objectCards":
                    DebugServerEndpoint.setObjectCardsPlantUmlImage(base64Diagram);
                    DebugServerEndpoint.sendDebugInfo(OBJECT_CARDS_PREFIX + base64Diagram);
                    break;
                case "objectDiagram":
                    DebugServerEndpoint.setObjectDiagramPlantUmlImage(base64Diagram);
                    DebugServerEndpoint.sendDebugInfo(OBJECT_DIAGRAM_PREFIX + base64Diagram);
                    break;
                case "classDiagram":
                    DebugServerEndpoint.setClassDiagramPlantUmlImage(base64Diagram);
                    break;
                default:
                    LOGGER.warn("Unknown diagram type: " + type);
            }
        } catch (IOException e) {
            LOGGER.error("Error generating " + type + " PlantUML diagram", e);
            throw new RuntimeException(e);
        }
    }

}
