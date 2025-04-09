package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer;
import de.code14.edupydebugger.analysis.staticanalysis.PythonAnalyzer;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.diagram.ClassDiagramParser;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String VARIABLES_PREFIX = "variables:";
    private static final String OBJECT_CARDS_PREFIX = "oc:";
    private static final String OBJECT_DIAGRAM_PREFIX = "od:";

    private final XDebugProcess debugProcess;
    private final XDebugSession session;

    private final ClassDiagramParser classDiagramParser;

    // Set of default Python types that are not considered as references
    public static final Set<String> defaultTypes = new HashSet<>() {{
        add("int");
        add("float");
        add("str");
        add("bool");
        add("list");
        add("dict");
        add("tuple");
        add("set");
    }};


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
        Map<PyThreadInfo, List<PyStackFrame>> perThreadFrames = DebuggerUtils.getStackFramesPerThread(this.session);

        List<PyStackFrame> firstPyStackFrames = null;

        // Thread-Namen und zugehörige Methodenstacks ausgeben
        LOGGER.debug("=== Thread- und Stack-Informationen ===");
        for (Map.Entry<PyThreadInfo, List<PyStackFrame>> entry : perThreadFrames.entrySet()) {
            PyThreadInfo threadInfo = entry.getKey();
            List<PyStackFrame> frames = entry.getValue();

            if (firstPyStackFrames == null) {
                firstPyStackFrames = frames;
            }

            // Thread-Namen und ID
            LOGGER.debug("Thread: " + threadInfo.getName() + " (ID: " + threadInfo.getId() + ")");

            // Methodennamen ausgeben (Reihenfolge: erstes Element ist der "Top-Frame")
            for (PyStackFrame frame : frames) {
                String methodName = frame.getName();
                LOGGER.debug("   -> " + methodName);
            }
        }

        StackFrameAnalyzer stackFrameAnalyzer = new StackFrameAnalyzer(firstPyStackFrames);

        stackFrameAnalyzer.analyzeFrames();

        // Handle variables and objects extracted from the stack frames
        Map<String, ObjectInfo> objects = handleObjects(stackFrameAnalyzer);
        handleVariables(stackFrameAnalyzer, objects);
    }

    /**
     * Handles the collection and sending of variable information.
     * Converts the variable data to a string format and sends it to the WebSocket server.
     *
     * @param stackFrameAnalyzer the analyzer responsible for processing stack frames
     */
    private void handleVariables(StackFrameAnalyzer stackFrameAnalyzer, Map<String, ObjectInfo> objects) {
        Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();
        StringBuilder variablesString = new StringBuilder();

        // Construct the variables string in the format "key=value;"
        for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
            if (entry.getValue().size() > 2) {
                String variableType = entry.getValue().get(1); // Take the type in the list
                if (!defaultTypes.contains(variableType)) { // If it is a complex type further infos needed (attributes)
                    ObjectInfo currentObject = objects.get(entry.getKey()); // Extract the ObjectInfo
                    List<AttributeInfo> currentAttributeInfos = currentObject.attributes();

                    // Form of the variable string -> id=name,type,attr1|val1###attr2|val2###...,scope;
                    variablesString.append(entry.getKey())
                            .append("=")
                            .append(entry.getValue().get(0))
                            .append(",")
                            .append(variableType)
                            .append(",");

                    for (AttributeInfo attributeInfo : currentAttributeInfos) {
                        variablesString.append(attributeInfo.name()).append("|").append(attributeInfo.value()).append("###");
                    }

                    variablesString.append(",").append(entry.getValue().get(3)).append(";");
                } else { // For default Python types string style -> id=name,type,value,scope;
                    String values = String.join(",", entry.getValue());
                    variablesString.append(entry.getKey()).append("=").append(values).append(";");
                }
            }
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
    private Map<String, ObjectInfo> handleObjects(StackFrameAnalyzer stackFrameAnalyzer) {
        Map<String, ObjectInfo> objects = stackFrameAnalyzer.getObjects();

        // Generate the object cards diagram in PlantUML format and send it
        Map<String, String> objectCardPlantUmlStrings = ObjectDiagramParser.generateObjectCards(objects);
        generateAndSendObjectCards(objectCardPlantUmlStrings);

        // Generate the object relationships diagram in PlantUML format
        String objectDiagramPlantUmlString = ObjectDiagramParser.generateObjectDiagram(objects);
        generateAndUpdateDiagramInServerEndpoint(objectDiagramPlantUmlString, "objectDiagram");

        return objects;
    }

    /**
     * Performs static code analysis and updates the class diagram.
     * This method generates a class diagram in PlantUML format and sends it to the WebSocket server.
     *
     * @param pyDebugProcess the Python debug process
     */
    private void performStaticAnalysis(PyDebugProcess pyDebugProcess) {
        String classDiagramPlantUmlString = classDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
        generateAndUpdateDiagramInServerEndpoint(classDiagramPlantUmlString, "classDiagram");
    }

    /**
     * Generates a PlantUML diagram and updates it in the debug server endpoint for get requests.
     *
     * @param plantUmlString the PlantUML string to generate the diagram from
     * @param type           the type of diagram being generated ("objectDiagram", "classDiagram")
     */
    private void generateAndUpdateDiagramInServerEndpoint(String plantUmlString, String type) {
        try {
            String base64Diagram = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlString);
            switch (type) {
                case "objectDiagram":
                    DebugServerEndpoint.setObjectDiagramPlantUmlImage(base64Diagram);
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

    /**
     * Generates PlantUML diagrams for object cards from the given map of PlantUML strings,
     * and sends these diagrams to the WebSocket server.
     * <p>
     * The diagrams are encoded as Base64 strings and formatted as follows:
     * Each entry consists of the key and the corresponding Base64 diagram, separated by "|".
     * Multiple entries are concatenated with "###" as the delimiter.
     * The complete string is prefixed with {@link #OBJECT_CARDS_PREFIX}.
     * <p>
     * The resulting string is then sent to the WebSocket server for visualization.
     *
     * @param objectCardPlantUmlStrings a map where the key is the object identifier and the value is the PlantUML string representing the object card
     */
    private void generateAndSendObjectCards(Map<String, String> objectCardPlantUmlStrings) {
        StringBuilder objectCardPlantUmlImagesData = new StringBuilder();
        objectCardPlantUmlImagesData.append(OBJECT_CARDS_PREFIX);

        objectCardPlantUmlStrings.forEach((key, plantUmlString) -> {
            try {
                String base64Diagram = PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlString);
                objectCardPlantUmlImagesData.append(key).append("|").append(base64Diagram).append("###");
            } catch (IOException e) {
                LOGGER.error("Error generating object card (PlantUML)", e);
                throw new RuntimeException(e);
            }
        });

        DebugServerEndpoint.setObjectCardPlantUmlImagesData(objectCardPlantUmlImagesData.toString());
        DebugServerEndpoint.sendDebugInfo(objectCardPlantUmlImagesData.toString());
    }

}
