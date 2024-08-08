package de.code14.edupydebugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.debugger.DebuggerUtils;
import de.code14.edupydebugger.debugger.StackFrameAnalyzer;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.ui.ClassDiagramParser;
import de.code14.edupydebugger.ui.ObjectDiagramParser;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author julian
 * @version 1.0
 * @since 05.07.24
 */
public class DebugSessionListener implements XDebugSessionListener {

    private final static Logger LOGGER = Logger.getInstance(DebugSessionListener.class);

    private final XDebugProcess debugProcess;
    private final XDebugSession session;


    public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
        this.debugProcess = debugProcess;
        this.session = debugProcess.getSession();
    }


    @Override
    public void stackFrameChanged() {
        LOGGER.info("stackFrameChanged");

        if (debugProcess instanceof PyDebugProcess pyDebugProcess) {
            // Dynamische Analyse
            List<PyStackFrame> pyStackFrames = DebuggerUtils.getAllStackFrames(this.session);
            StackFrameAnalyzer stackFrameAnalyzer = new StackFrameAnalyzer(pyStackFrames);

            stackFrameAnalyzer.analyzeFrames();
            // Zugriff auf die gesammelten Daten
            Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();
            StringBuilder variablesString = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
                String values = String.join(",", entry.getValue());
                variablesString.append(entry.getKey()).append("=").append(values).append(";");
            }
            LOGGER.info(variablesString.toString());
            DebugServerEndpoint.setVariablesString(variablesString.toString());
            DebugServerEndpoint.sendDebugInfo("variables:" + variablesString.toString());

            Map<String, List<Object>[]> objects = stackFrameAnalyzer.getObjects();
            String objectCardsPlantUmlString = ObjectDiagramParser.generateObjectCards(objects);
            try {
                DebugServerEndpoint.setObjectCardsPlantUmlImage(PlantUMLDiagramGenerator.generateDiagramAsBase64(objectCardsPlantUmlString));
                DebugServerEndpoint.sendDebugInfo("oc:" + PlantUMLDiagramGenerator.generateDiagramAsBase64(objectCardsPlantUmlString));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            /*for (Map.Entry<String, List<String>[]> entry : objects.entrySet()) {
                System.out.println(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
            }*/


            // Statische Code-Analyse
            String classDiagramPlantUmlString = ClassDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
            try {
                DebugServerEndpoint.setClassDiagramPlantUmlImage(PlantUMLDiagramGenerator.generateDiagramAsBase64(classDiagramPlantUmlString));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



}
