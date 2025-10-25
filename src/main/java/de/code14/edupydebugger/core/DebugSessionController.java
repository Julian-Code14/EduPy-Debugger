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
import de.code14.edupydebugger.server.dto.*;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Coordinates the dynamic analysis of a suspended Python debugging session and
 * publishes the extracted data as JSON messages through the {@link DebugServerEndpoint}.
 * <p>
 * The {@code DebugSessionController} acts as the bridge between the PyCharm
 * {@link PyDebugProcess} and the EduPy Debugger’s visualization layer.
 * It collects stack frame, variable, and object information from the live
 * debugging session, transforms these data structures into DTOs, and transmits
 * them to the browser-based frontend.
 * <p>
 * Key responsibilities include:
 * <ul>
 *   <li>Analyzing stack frames per suspended thread</li>
 *   <li>Publishing call stack, variable table, and object representations as JSON</li>
 *   <li>Generating class/object diagrams via PlantUML and encoding them as Base64 SVG</li>
 * </ul>
 */
public class DebugSessionController {
    private static final Logger LOGGER = Logger.getInstance(DebugSessionController.class);

    private PyDebugProcess debugProcess;

    /**
     * A set of built-in Python types that are treated as primitive values when rendering variable data.
     */
    public static final Set<String> defaultTypes = new HashSet<>() {{
        add("int"); add("float"); add("str"); add("bool"); add("list");
        add("dict"); add("tuple"); add("set");
    }};

    /**
     * Performs a dynamic analysis of the current debugging session.
     * <p>
     * The method inspects all threads and their corresponding stack frames to identify
     * suspended states. It analyzes the most relevant thread (either the selected one
     * or the first suspended thread) and publishes the resulting call stack, variables,
     * and object diagrams to the frontend.
     *
     * @param selectedThread the name of the thread to analyze; if {@code null},
     *                       the first suspended thread will be used instead
     * @throws IOException if an error occurs during diagram generation or file processing
     */
    public void performDynamicAnalysis(@Nullable String selectedThread) throws IOException {
        Map<PyThreadInfo, List<PyStackFrame>> perThreadFrames =
                DebuggerUtils.getStackFramesPerThread(this.debugProcess.getSession());

        List<PyStackFrame> stackFrames = null;

        for (Map.Entry<PyThreadInfo, List<PyStackFrame>> entry : perThreadFrames.entrySet()) {
            PyThreadInfo threadInfo = entry.getKey();
            if (selectedThread != null) {
                if (threadInfo.getName().equals(selectedThread) && threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                    stackFrames = entry.getValue();
                    publishCallstack(entry.getValue());
                }
            } else if (stackFrames == null && threadInfo.getState() == PyThreadInfo.State.SUSPENDED) {
                stackFrames = entry.getValue();
            }
        }

        if (stackFrames != null) {
            StackFrameAnalyzer analyzer = new StackFrameAnalyzer(stackFrames);
            analyzer.analyzeFrames();

            Map<String, ObjectInfo> objects = handleObjects(analyzer);
            handleVariables(analyzer, objects);
        }
    }

    /**
     * Publishes the call stack for the currently analyzed thread.
     *
     * @param frames a list of stack frames belonging to the selected or first suspended thread
     */
    private void publishCallstack(List<PyStackFrame> frames) {
        CallstackPayload payload = new CallstackPayload();
        payload.frames = new ArrayList<>();
        for (PyStackFrame f : frames) payload.frames.add(f.getName());
        DebugServerEndpoint.publishCallstack(payload);
    }

    /**
     * Builds and publishes the list of variables visible in the current stack frame scope.
     * <p>
     * Primitive variables are rendered as plain strings, while complex objects
     * are summarized using their attributes for a compact table representation.
     *
     * @param analyzer the active {@link StackFrameAnalyzer} used to extract variable data
     * @param objects  a mapping of object IDs to {@link ObjectInfo} instances used for lookups
     */
    private void handleVariables(StackFrameAnalyzer analyzer, Map<String, ObjectInfo> objects) {
        Map<String, List<String>> varsRaw = analyzer.getVariables();
        List<VariableDTO> variables = new ArrayList<>();

        for (Map.Entry<String, List<String>> e : varsRaw.entrySet()) {
            List<String> v = e.getValue();
            if (v.size() < 4) continue;

            String id     = e.getKey();
            String names  = v.get(0);
            String type   = v.get(1);
            String value  = v.get(2);
            String scope  = v.get(3);

            VariableDTO dto = new VariableDTO();
            dto.id = id;
            dto.names = Arrays.asList(names.split("###"));
            dto.pyType = type;
            dto.scope = scope;

            ValueDTO val = new ValueDTO();
            if (defaultTypes.contains(type)) {
                val.kind = "primitive";
                val.repr = value.replace("~", ", ");
            } else {
                val.kind = "composite";
                // Kompakte Attribut-Repräsentation für die Tabelle (Detailanzeige via Objektkarten)
                ObjectInfo info = objects.get(id);
                StringBuilder repr = new StringBuilder();
                if (info != null) {
                    for (AttributeInfo a : info.attributes()) {
                        String shown = a.value();
                        if (shown.length() > 20) shown = shown.substring(0, 20) + " [...]";
                        repr.append(a.name()).append(": ").append(shown).append("\n");
                    }
                }
                val.repr = repr.toString().trim();
            }
            dto.value = val;

            variables.add(dto);
        }

        DebugServerEndpoint.publishVariables(new VariablesPayload(variables));
    }

    /**
     * Analyzes the current stack frame’s object references and generates visual representations.
     * <p>
     * Each object is converted into an individual “object card” diagram (Base64-encoded SVG),
     * and a complete object diagram is generated to show inter-object relationships.
     *
     * @param analyzer the {@link StackFrameAnalyzer} that extracted object metadata
     * @return a mapping from object ID to its corresponding {@link ObjectInfo}
     * @throws IOException if PlantUML diagram generation fails
     */
    private Map<String, ObjectInfo> handleObjects(StackFrameAnalyzer analyzer) throws IOException {
        Map<String, ObjectInfo> objects = analyzer.getObjects();

        // Objektkarten -> Base64 SVG je Objekt
        Map<String, String> cardsPuml = ObjectDiagramParser.generateObjectCards(objects);
        ObjectCardPayload ocPayload = new ObjectCardPayload();
        ocPayload.cards = new ArrayList<>();
        for (Map.Entry<String, String> entry : cardsPuml.entrySet()) {
            String base64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(entry.getValue());
            CardDTO c = new CardDTO();
            c.id = entry.getKey();
            c.svgBase64 = base64;
            ocPayload.cards.add(c);
        }
        DebugServerEndpoint.publishObjectCards(ocPayload);

        // Objektdiagramm
        String odPuml = ObjectDiagramParser.generateObjectDiagram(objects);
        String odBase64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(odPuml);
        DebugServerEndpoint.publishObjectDiagram(odBase64);

        return objects;
    }

    /**
     * Assigns the current {@link PyDebugProcess} to this controller.
     * This reference is used to retrieve the active debugging session and its stack frames.
     *
     * @param debugProcess the active PyCharm debug process
     */
    public void setDebugProcess(PyDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }
}