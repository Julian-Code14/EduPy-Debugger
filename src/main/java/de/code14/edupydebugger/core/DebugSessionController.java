package de.code14.edupydebugger.core;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyStackFrame;
import com.jetbrains.python.debugger.PyThreadInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author julian
 * @version 0.3.0
 * @since 0.3.0
 */
public class DebugSessionController {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionController.class);

    private static final String VARIABLES_PREFIX = "variables:";
    private static final String CALLSTACK_PREFIX = "callstack:";
    private static final String OBJECT_CARDS_PREFIX = "oc:";
    private static final String OBJECT_DIAGRAM_PREFIX = "od:";

    private PyDebugProcess debugProcess;

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
     * Performs a dynamic analysis of the currently suspended PyCharm debug session by analyzing the stack frames,
     * determining the call stack, and extracting variable information.
     *
     * <p>If a specific thread name is provided via <code>selectedThread</code>, only the stack frames of that thread
     * will be used, provided that the thread is suspended. Otherwise, empty values for variables and call stack
     * are sent to the {@link DebugServerEndpoint}. If <code>selectedThread</code> is <code>null</code>, the first
     * suspended thread (if any) will be used for the analysis.</p>
     *
     * <p>Afterwards, the {@link StackFrameAnalyzer} is used to analyze the selected stack frames, and the resulting
     * variable and object information is processed and sent to the WebSocket server.</p>
     *
     * @param selectedThread the name of the selected thread; may be <code>null</code> to pick the first suspended thread
     * @throws IOException if an error occurs while generating or sending the debug information
     *
     * @see StackFrameAnalyzer
     * @see DebuggerUtils
     */
    public void performDynamicAnalysis(@Nullable String selectedThread) throws IOException {
        Map<PyThreadInfo, List<PyStackFrame>> perThreadFrames = DebuggerUtils.getStackFramesPerThread(this.debugProcess.getSession());

        List<PyStackFrame> stackFrames = null;

        for (Map.Entry<PyThreadInfo, List<PyStackFrame>> entry : perThreadFrames.entrySet()) {
            PyThreadInfo threadInfo = entry.getKey();

            if (selectedThread != null) {
                if (threadInfo.getName().equals(selectedThread) && threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                    stackFrames = entry.getValue();
                    generateCallStackString(stackFrames);
                } else {
                    DebugServerEndpoint.setVariablesString("");
                    DebugServerEndpoint.sendDebugInfo(VARIABLES_PREFIX);

                    DebugServerEndpoint.setCallStackString("");
                    DebugServerEndpoint.sendDebugInfo(CALLSTACK_PREFIX);
                }
            } else {
                if (stackFrames == null && threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                    stackFrames = entry.getValue();
                }
            }
        }

        if (stackFrames != null) {
            StackFrameAnalyzer stackFrameAnalyzer = new StackFrameAnalyzer(stackFrames);
            stackFrameAnalyzer.analyzeFrames();

            // Handle variables and objects extracted from the stack frames
            Map<String, ObjectInfo> objects = handleObjects(stackFrameAnalyzer);
            handleVariables(stackFrameAnalyzer, objects);
        }
    }

    /**
     * Constructs a semicolon-separated string of stack frame names from the given list of {@link PyStackFrame}
     * objects and sends it to the {@link DebugServerEndpoint} as call stack information.
     * <p>
     * For each frame in the provided list, its name is appended to a {@link StringBuilder} separated by semicolons.
     * The resulting string is then:
     * <ul>
     *   <li>Stored in the debug server endpoint via {@link DebugServerEndpoint#setCallStackString(String)}.</li>
     *   <li>Sent to the WebSocket client with the <em>CALLSTACK_PREFIX</em> prepended.</li>
     * </ul>
     *
     * @param stackFrames the list of {@link PyStackFrame} instances to be converted into a call stack string
     * @see DebugServerEndpoint
     * @see PyStackFrame
     */
    private void generateCallStackString(List<PyStackFrame> stackFrames) {
        StringBuilder callStackString = new StringBuilder();
        for (PyStackFrame frame : stackFrames) {
            callStackString.append(frame.getName()).append(";");
        }

        DebugServerEndpoint.setCallStackString(callStackString.toString());
        DebugServerEndpoint.sendDebugInfo(CALLSTACK_PREFIX + callStackString);
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
                        variablesString.append(attributeInfo.name()).append("|");
                        if (attributeInfo.value().length() > 20) {
                            variablesString.append(attributeInfo.value(), 0, 20).append(" [...]");
                        } else {
                            variablesString.append(attributeInfo.value());
                        }
                        variablesString.append("###");
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
    private Map<String, ObjectInfo> handleObjects(StackFrameAnalyzer stackFrameAnalyzer) throws IOException {
        Map<String, ObjectInfo> objects = stackFrameAnalyzer.getObjects();

        // Generate the object cards diagram in PlantUML format and send it
        Map<String, String> objectCardPlantUmlStrings = ObjectDiagramParser.generateObjectCards(objects);
        generateAndSendObjectCards(objectCardPlantUmlStrings);

        // Generate the object relationships diagram in PlantUML format
        String objectDiagramPlantUmlString = ObjectDiagramParser.generateObjectDiagram(objects);
        generateAndUpdateObjectDiagramInServerEndpoint(objectDiagramPlantUmlString);

        return objects;
    }

    /**
     * Generates a PlantUML object diagram and updates it in the debug server endpoint for get requests.
     *
     * @param objectDiagramPlantUmlString the PlantUML string to generate the object diagram from
     */
    private void generateAndUpdateObjectDiagramInServerEndpoint(String objectDiagramPlantUmlString) throws IOException {
        String base64Diagram = PlantUMLDiagramGenerator.generateDiagramAsBase64(objectDiagramPlantUmlString);
        DebugServerEndpoint.setObjectDiagramPlantUmlImage(base64Diagram);
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

    public void setDebugProcess(PyDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

}
