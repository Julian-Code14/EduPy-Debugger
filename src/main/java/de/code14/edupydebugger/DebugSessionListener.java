package de.code14.edupydebugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.frame.*;
import com.jetbrains.python.debugger.*;
import com.jetbrains.python.psi.types.TypeEvalContext;
import de.code14.edupydebugger.debugger.DebuggerUtils;
import de.code14.edupydebugger.debugger.PythonAnalyzer;
import de.code14.edupydebugger.debugger.StackFrameAnalyzer;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.ui.ClassDiagramParser;
import de.code14.edupydebugger.ui.ObjectDiagramParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
            StringBuilder variablesString = new StringBuilder("variables:");
            for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
                String values = String.join(",", entry.getValue());
                variablesString.append(entry.getKey()).append("=").append(values).append(";");
            }
            LOGGER.debug(variablesString.toString());
            DebugServerEndpoint.sendDebugInfo(variablesString.toString());

            Map<String, List<Object>[]> objects = stackFrameAnalyzer.getObjects();
            String plantUmlString = ObjectDiagramParser.generateObjectCards(objects);
            try {
                DebugServerEndpoint.sendDebugInfo("oc:" + PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUmlString));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            /*for (Map.Entry<String, List<String>[]> entry : objects.entrySet()) {
                System.out.println(entry.getKey() + ": " + Arrays.toString(entry.getValue()));
            }*/


            // Statische Code-Analyse
            String plantUml = ClassDiagramParser.generateClassDiagram(pyDebugProcess.getProject());
            try {
                DebugServerEndpoint.setClassDiagramPlantUml(PlantUMLDiagramGenerator.generateDiagramAsBase64(plantUml));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



}
