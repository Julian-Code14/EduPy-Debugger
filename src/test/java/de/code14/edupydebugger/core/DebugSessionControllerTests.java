package de.code14.edupydebugger.core;

import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.*;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Tests for the DebugSessionController class,
 * which performs dynamic analysis (variables, objects, etc.).
 *
 * @author julian
 * @version 0.3.0
 * @since 0.3.0
 */
public class DebugSessionControllerTests {

    @Mock
    private PyDebugProcess mockPyDebugProcess; // Mock for PyDebugProcess
    @Mock
    private XDebugSession mockXDebugSession; // Mock for XDebugSession

    private DebugSessionController debugSessionController; // System under test

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        // Stub PyDebugProcess to return the mocked XDebugSession.
        when(mockPyDebugProcess.getSession()).thenReturn(mockXDebugSession);
        // Stub XDebugSession to return the same PyDebugProcess.
        when(mockXDebugSession.getDebugProcess()).thenReturn(mockPyDebugProcess);
        // Instantiate DebugSessionController.
        debugSessionController = new DebugSessionController();
        // Set the debug process.
        debugSessionController.setDebugProcess(mockPyDebugProcess);
    }

    @Test
    public void testPerformDynamicAnalysis_NoThreadsSuspended() throws Exception {
        // Create a mock thread in the RUNNING state.
        PyThreadInfo runningThread = mock(PyThreadInfo.class);
        when(runningThread.getName()).thenReturn("Thread-1");
        when(runningThread.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        // Create a map with the running thread mapping to an empty list of frames.
        Map<PyThreadInfo, List<PyStackFrame>> mockFramesMap = new HashMap<>();
        mockFramesMap.put(runningThread, Collections.emptyList());

        // Mock static methods and construction of StackFrameAnalyzer to avoid side effects.
        try (MockedStatic<DebuggerUtils> debuggerUtilsMock = mockStatic(DebuggerUtils.class);
             MockedStatic<DebugServerEndpoint> debugEndpointMock = mockStatic(DebugServerEndpoint.class);
             MockedConstruction<StackFrameAnalyzer> analyzerConstruction =
                     mockConstruction(StackFrameAnalyzer.class,
                             (mockAnalyzer, context) -> {
                                 // Stub analyzeFrames() to do nothing.
                                 doNothing().when(mockAnalyzer).analyzeFrames();
                             }
                     )) {

            // Stub DebuggerUtils.getStackFramesPerThread to return the mock frames map.
            debuggerUtilsMock.when(() -> DebuggerUtils.getStackFramesPerThread(mockXDebugSession))
                    .thenReturn(mockFramesMap);

            // Invoke performDynamicAnalysis with a null selected thread.
            debugSessionController.performDynamicAnalysis(null);

            // Verify that DebugServerEndpoint.setVariablesString() is called with an empty string.
            debugEndpointMock.verify(() -> DebugServerEndpoint.setVariablesString(""), times(1));
            // Verify that DebugServerEndpoint.sendDebugInfo() is called with "variables:".
            debugEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo("variables:"), times(1));

            // debugEndpointMock.verify(() -> DebugServerEndpoint.setCallStackString(""), times(1));
            // debugEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo("callstack:"), times(1));
        }
    }

    @Test
    public void testPerformDynamicAnalysis_SelectedThreadSuspended() throws Exception {
        // Create a mock running thread.
        PyThreadInfo runningThread = mock(PyThreadInfo.class);
        when(runningThread.getName()).thenReturn("Thread-1");
        when(runningThread.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        // Create a mock suspended thread.
        PyThreadInfo suspendedThread = mock(PyThreadInfo.class);
        when(suspendedThread.getName()).thenReturn("MeinThread");
        when(suspendedThread.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);

        // Create two mock stack frames.
        PyStackFrame frame1 = mock(PyStackFrame.class);
        when(frame1.getName()).thenReturn("frame1");
        PyStackFrame frame2 = mock(PyStackFrame.class);
        when(frame2.getName()).thenReturn("frame2");

        // Create a map with the running thread mapping to an empty list and the suspended thread mapping to a list with two frames.
        Map<PyThreadInfo, List<PyStackFrame>> mockFramesMap = new HashMap<>();
        mockFramesMap.put(runningThread, Collections.emptyList());
        mockFramesMap.put(suspendedThread, Arrays.asList(frame1, frame2));

        // Mock static methods and construction of StackFrameAnalyzer.
        try (MockedStatic<DebuggerUtils> debuggerUtilsMock = mockStatic(DebuggerUtils.class);
             MockedStatic<DebugServerEndpoint> debugEndpointMock = mockStatic(DebugServerEndpoint.class);
             MockedConstruction<StackFrameAnalyzer> analyzerConstruction =
                     mockConstruction(StackFrameAnalyzer.class,
                             (mockAnalyzer, context) -> {
                                 // Stub analyzeFrames() to do nothing.
                                 doNothing().when(mockAnalyzer).analyzeFrames();
                             }
                     )) {

            // Stub DebuggerUtils.getStackFramesPerThread to return the mock map.
            debuggerUtilsMock.when(() -> DebuggerUtils.getStackFramesPerThread(mockXDebugSession))
                    .thenReturn(mockFramesMap);

            // Invoke performDynamicAnalysis with the selected thread "MeinThread".
            debugSessionController.performDynamicAnalysis("MeinThread");

            // Verify that the call stack is set to "frame1;frame2;".
            debugEndpointMock.verify(
                    () -> DebugServerEndpoint.setCallStackString("frame1;frame2;"), times(1)
            );
            // Verify that the call stack info is sent.
            debugEndpointMock.verify(
                    () -> DebugServerEndpoint.sendDebugInfo("callstack:frame1;frame2;"), times(1)
            );
        }
    }

    /**
     * Tests the private method generateCallStackString by reflection.
     */
    @Test
    public void testGenerateCallStackStringReflected() throws Exception {
        // Mock static method calls of DebugServerEndpoint.
        try (MockedStatic<DebugServerEndpoint> debugEndpointMock = mockStatic(DebugServerEndpoint.class)) {

            // Obtain a reference to the private generateCallStackString(List<PyStackFrame>) method.
            Method generateCallStackMethod = DebugSessionController.class
                    .getDeclaredMethod("generateCallStackString", List.class);
            generateCallStackMethod.setAccessible(true);

            // Create two mock stack frames.
            PyStackFrame frameA = mock(PyStackFrame.class);
            when(frameA.getName()).thenReturn("fA");
            PyStackFrame frameB = mock(PyStackFrame.class);
            when(frameB.getName()).thenReturn("fB");
            List<PyStackFrame> frames = Arrays.asList(frameA, frameB);

            // Invoke the private method.
            generateCallStackMethod.invoke(debugSessionController, frames);

            // Verify that DebugServerEndpoint.setCallStackString is called with "fA;fB;".
            debugEndpointMock.verify(() -> DebugServerEndpoint.setCallStackString("fA;fB;"), times(1));
            // Verify that DebugServerEndpoint.sendDebugInfo is called with "callstack:fA;fB;".
            debugEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo("callstack:fA;fB;"), times(1));
        }
    }

    /**
     * Tests the private method generateAndSendObjectCards via reflection.
     */
    @Test
    public void testGenerateAndSendObjectCardsReflected() throws Exception {
        // Obtain a reference to the private generateAndSendObjectCards(Map<String,String>) method.
        Method method = DebugSessionController.class
                .getDeclaredMethod("generateAndSendObjectCards", Map.class);
        method.setAccessible(true);

        // Create a mock map for object cards.
        Map<String, String> mockObjectCards = new LinkedHashMap<>();
        mockObjectCards.put("Obj1", "somePlantUML1");
        mockObjectCards.put("Obj2", "somePlantUML2");

        // Mock static methods for PlantUMLDiagramGenerator and DebugServerEndpoint.
        try (MockedStatic<PlantUMLDiagramGenerator> plantUMLMock = mockStatic(PlantUMLDiagramGenerator.class);
             MockedStatic<DebugServerEndpoint> debugEndpointMock = mockStatic(DebugServerEndpoint.class)) {

            // Stub conversion from PlantUML to base64.
            plantUMLMock.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64("somePlantUML1"))
                    .thenReturn("base64encodedA");
            plantUMLMock.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64("somePlantUML2"))
                    .thenReturn("base64encodedB");

            // Invoke the private method.
            method.invoke(debugSessionController, mockObjectCards);

            // Expected result for object cards.
            String expected = "oc:Obj1|base64encodedA###Obj2|base64encodedB###";
            // Verify that the expected static methods are called.
            debugEndpointMock.verify(() -> DebugServerEndpoint.setObjectCardPlantUmlImagesData(expected), times(1));
            debugEndpointMock.verify(() -> DebugServerEndpoint.sendDebugInfo(expected), times(1));
        }
    }
}