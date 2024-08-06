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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
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
            /*ExecutorService executorService = Executors.newSingleThreadExecutor();
            Callable<Void> task = () -> {
                stackFrameAnalyzer.analyzeFrames();
                return null;
            };
            Future<Void> future = executorService.submit(task);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            executorService.shutdown();*/

            stackFrameAnalyzer.analyzeFrames();
            // Zugriff auf die gesammelten Daten
            Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();
            for (Map.Entry<String, List<String>> entry : variables.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }

            Map<String, List<List<String>>> attributes = stackFrameAnalyzer.getAttributes();
            for (Map.Entry<String, List<List<String>>> entry : attributes.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }


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
