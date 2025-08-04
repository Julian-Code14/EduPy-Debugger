package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.debugger.PyStackFrame;

import java.util.*;

/**
 * The StackFrameAnalyzer class coordinates the analysis of stack frames during a debugging session.
 * It delegates the task of analyzing variables and objects to separate analyzers.
 */
public class StackFrameAnalyzer {

    private static final Logger LOGGER = Logger.getInstance(StackFrameAnalyzer.class);


    private final VariableAnalyzer variableAnalyzer;
    private final ObjectAnalyzer objectAnalyzer;



    /**
     * Constructor for StackFrameAnalyzer.
     *
     * @param pyStackFrames a list of Python stack frames to analyze
     */
    public StackFrameAnalyzer(List<PyStackFrame> pyStackFrames) {
        this.variableAnalyzer = new VariableAnalyzer(pyStackFrames);
        this.objectAnalyzer = new ObjectAnalyzer(pyStackFrames);
    }

    /**
     * Analyzes the stack frames to collect information about variables and objects.
     * This method uses CountDownLatch to ensure that all frames are processed before proceeding.
     */
    public void analyzeFrames() {
        LOGGER.debug("Starting analysis of stack frames.");

        variableAnalyzer.analyzeVariables();
        objectAnalyzer.analyzeObjects();

        LOGGER.debug("Analysis of stack frames completed.");
    }


    /**
     * Returns the collected variables map.
     *
     * @return a map where the key is the variable ID and the value is a list containing the name, type, current value, and scope
     */
    public Map<String, List<String>> getVariables() {
        return variableAnalyzer.getVariables();
    }

    /**
     * Returns the collected objects map.
     *
     * @return a map where the key is the object ID and the value is an array containing the name, type, current value, and visibility
     */
    public Map<String, ObjectInfo> getObjects() {
        return objectAnalyzer.getObjects();
    }

}
