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
 * Listener class that responds to changes in the debug session's stack frame.
 * This class triggers both static and dynamic code analysis when the stack frame changes.
 *
 * @author julian
 * @version 1.0
 * @since 05.07.24
 */
public class DebugSessionListener implements XDebugSessionListener {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    private final XDebugProcess debugProcess;
    private final XDebugSession session;
    private final ClassDiagramParser classDiagramParser;


    public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        this.session = debugProcess.getSession();

        this.classDiagramParser = new ClassDiagramParser(new PythonAnalyzer());
    }


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
     * Performs dynamic analysis using the StackFrameAnalyzer.
     */
    private void performDynamicAnalysis() {
        List<PyStackFrame> pyStackFrames = DebuggerUtils.getAllStackFrames(this.session);
        StackFrameAnalyzer stackFrameAnalyzer = new StackFrameAnalyzer(pyStackFrames);

        stackFrameAnalyzer.analyzeFrames();

        // Handle variables and objects
        handleVariables(stackFrameAnalyzer);
        handleObjects(stackFrameAnalyzer);
    }

    /**
     * Handles the collection and sending of variable information.
     *
     * @param stackFrameAnalyzer the analyzer responsible for processing stack frames
     */
    private void handleVariables(StackFrameAnalyzer stackFrameAnalyzer) {
        Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();
        StringBuilder variablesString = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
            String values = String.join(",", entry.getValue());
            variablesString.append(entry.getKey()).append("=").append(values).append(";");
        }

        LOGGER.info(variablesString.toString());
        DebugServerEndpoint.setVariablesString(variablesString.toString());
        DebugServerEndpoint.sendDebugInfo("variables:" + variablesString.toString());
    }

    /**
     * Handles the collection and sending of object information.
     *
     * @param stackFrameAnalyzer the analyzer responsible for processing stack frames
     */
    private void handleObjects(StackFrameAnalyzer stackFrameAnalyzer) {
        Map<String, ObjectInfo> objects = stackFrameAnalyzer.getObjects();

        String objectCardsPlantUmlString = ObjectDiagramParser.generateObjectCards(objects);
        try {
            String objectCardsBase64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(objectCardsPlantUmlString);
            DebugServerEndpoint.setObjectCardsPlantUmlImage(objectCardsBase64);
            DebugServerEndpoint.sendDebugInfo("oc:" + objectCardsBase64);
        } catch (IOException e) {
            LOGGER.error("Error generating object cards PlantUML diagram", e);
            throw new RuntimeException(e);
        }

        String objectDiagramPlantUmlString = ObjectDiagramParser.generateObjectDiagram(objects);
        try {
            String objectDiagramBase64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(objectDiagramPlantUmlString);
            DebugServerEndpoint.setObjectDiagramPlantUmlImage(objectDiagramBase64);
            DebugServerEndpoint.sendDebugInfo("od:" + objectDiagramBase64);
        } catch (IOException e) {
            LOGGER.error("Error generating object diagram PlantUML diagram", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs static code analysis and updates the class diagram.
     *
     * @param pyDebugProcess the Python debug process
     */
    private void performStaticAnalysis(PyDebugProcess pyDebugProcess) {
        String classDiagramPlantUmlString = classDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
        try {
            String classDiagramBase64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(classDiagramPlantUmlString);
            DebugServerEndpoint.setClassDiagramPlantUmlImage(classDiagramBase64);
        } catch (IOException e) {
            LOGGER.error("Error generating class diagram PlantUML diagram", e);
            throw new RuntimeException(e);
        }
    }



}
