package de.code14.edupydebugger.core;

import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyStackFrame;
import com.jetbrains.python.debugger.PyThreadInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.CallstackPayload;
import de.code14.edupydebugger.server.dto.ObjectCardPayload;
import de.code14.edupydebugger.server.dto.VariablesPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DebugSessionControllerTests {

    @Mock private PyDebugProcess mockPyDebugProcess;
    @Mock private XDebugSession mockXDebugSession;

    private DebugSessionController sut;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockPyDebugProcess.getSession()).thenReturn(mockXDebugSession);
        when(mockXDebugSession.getDebugProcess()).thenReturn(mockPyDebugProcess);

        sut = new DebugSessionController();
        sut.setDebugProcess(mockPyDebugProcess);
    }

    @Test
    public void testPerformDynamicAnalysis_NoThreadsSuspended_doesNotPublishAnything() throws Exception {
        // Thread RUNNING, keine Frames
        PyThreadInfo running = mock(PyThreadInfo.class);
        when(running.getName()).thenReturn("Thread-1");
        when(running.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        Map<PyThreadInfo, List<PyStackFrame>> framesMap = new HashMap<>();
        framesMap.put(running, Collections.emptyList());

        try (MockedStatic<DebuggerUtils> dbg = mockStatic(DebuggerUtils.class);
             MockedStatic<DebugServerEndpoint> endpoint = mockStatic(DebugServerEndpoint.class);
             MockedConstruction<StackFrameAnalyzer> analyzerCtor = mockConstruction(StackFrameAnalyzer.class)) {

            dbg.when(() -> DebuggerUtils.getStackFramesPerThread(mockXDebugSession)).thenReturn(framesMap);

            sut.performDynamicAnalysis(null);

            // Keine Publikationen, weil kein SUSPENDED-Thread/keine Frames
            endpoint.verifyNoInteractions();
            assertEquals("Analyzer should not be constructed when no frames exist", 0, analyzerCtor.constructed().size());
        }
    }

    @Test
    public void testPerformDynamicAnalysis_SelectedThreadSuspended_publishesCallstackAndDiagramsAndVariables() throws Exception {
        // RUNNING + SUSPENDED mit 2 Frames
        PyThreadInfo running = mock(PyThreadInfo.class);
        when(running.getName()).thenReturn("Thread-1");
        when(running.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        PyThreadInfo suspended = mock(PyThreadInfo.class);
        when(suspended.getName()).thenReturn("MeinThread");
        when(suspended.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);

        PyStackFrame f1 = mock(PyStackFrame.class);
        when(f1.getName()).thenReturn("frame1");
        PyStackFrame f2 = mock(PyStackFrame.class);
        when(f2.getName()).thenReturn("frame2");

        Map<PyThreadInfo, List<PyStackFrame>> framesMap = new HashMap<>();
        framesMap.put(running, Collections.emptyList());
        framesMap.put(suspended, Arrays.asList(f1, f2));

        // Fake-Rückgaben für Analyzer / Diagramm-Generatoren
        Map<String, String> fakeCardsPuml = new LinkedHashMap<>();
        fakeCardsPuml.put("Obj1", "puml1");
        fakeCardsPuml.put("Obj2", "puml2");

        String fakeObjectDiagramPuml = "objectDiagramPuml";

        try (MockedStatic<DebuggerUtils> dbg = mockStatic(DebuggerUtils.class);
             MockedStatic<ObjectDiagramParser> odp = mockStatic(ObjectDiagramParser.class);
             MockedStatic<PlantUMLDiagramGenerator> plant = mockStatic(PlantUMLDiagramGenerator.class);
             MockedStatic<DebugServerEndpoint> endpoint = mockStatic(DebugServerEndpoint.class);
             MockedConstruction<StackFrameAnalyzer> analyzerCtor =
                     mockConstruction(StackFrameAnalyzer.class, (mockAnalyzer, ctx) -> {
                         doNothing().when(mockAnalyzer).analyzeFrames();
                         // Leere Variablen, damit wir nur prüfen, dass publishVariables überhaupt aufgerufen wird
                         when(mockAnalyzer.getVariables()).thenReturn(Collections.emptyMap());
                         // Leere Objektmenge – die Karten/Diagramme kommen aus den statischen Parser-Mocks
                         when(mockAnalyzer.getObjects()).thenReturn(Collections.emptyMap());
                     })) {

            dbg.when(() -> DebuggerUtils.getStackFramesPerThread(mockXDebugSession)).thenReturn(framesMap);

            // Object cards + object diagram werden über die Parser-/Generator-Kette erzeugt
            odp.when(() -> ObjectDiagramParser.generateObjectCards(anyMap())).thenReturn(fakeCardsPuml);
            odp.when(() -> ObjectDiagramParser.generateObjectDiagram(anyMap())).thenReturn(fakeObjectDiagramPuml);

            plant.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64("puml1")).thenReturn("b64-1");
            plant.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64("puml2")).thenReturn("b64-2");
            plant.when(() -> PlantUMLDiagramGenerator.generateDiagramAsBase64("objectDiagramPuml")).thenReturn("b64-od");

            // Act
            sut.performDynamicAnalysis("MeinThread");

            // Assert: Callstack veröffentlicht
            ArgumentCaptor<CallstackPayload> callstackCap = ArgumentCaptor.forClass(CallstackPayload.class);
            endpoint.verify(() -> DebugServerEndpoint.publishCallstack(callstackCap.capture()), times(1));
            assertEquals(Arrays.asList("frame1", "frame2"), callstackCap.getValue().frames);

            // Object-Cards veröffentlicht (2 Karten)
            ArgumentCaptor<ObjectCardPayload> cardsCap = ArgumentCaptor.forClass(ObjectCardPayload.class);
            endpoint.verify(() -> DebugServerEndpoint.publishObjectCards(cardsCap.capture()), times(1));
            assertNotNull(cardsCap.getValue());
            assertNotNull(cardsCap.getValue().cards);
            assertEquals(2, cardsCap.getValue().cards.size());
            // Reihenfolge gemäß fakeCardsPuml:
            assertEquals("Obj1", cardsCap.getValue().cards.get(0).id);
            assertEquals("b64-1", cardsCap.getValue().cards.get(0).svgBase64);
            assertEquals("Obj2", cardsCap.getValue().cards.get(1).id);
            assertEquals("b64-2", cardsCap.getValue().cards.get(1).svgBase64);

            // Objekt-Diagramm veröffentlicht
            endpoint.verify(() -> DebugServerEndpoint.publishObjectDiagram("b64-od"), times(1));

            // Variables veröffentlicht (leer in diesem Test)
            ArgumentCaptor<VariablesPayload> varsCap = ArgumentCaptor.forClass(VariablesPayload.class);
            endpoint.verify(() -> DebugServerEndpoint.publishVariables(varsCap.capture()), times(1));
            assertNotNull(varsCap.getValue());
            assertNotNull(varsCap.getValue().variables);
            assertTrue(varsCap.getValue().variables.isEmpty());

            // Analyzer wurde genau einmal konstruiert mit den beiden Frames
            assertEquals(1, analyzerCtor.constructed().size());
            // analyzeFrames() wurde aufgerufen (indirekt verifiziert durch Verwendung der Rückgaben)

            // Keine weiteren unerwarteten Endpoint-Publikationen
            endpoint.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testPerformDynamicAnalysis_FirstSuspendedThreadSelectedImplicitly_whenSelectedThreadNull() throws Exception {
        // Arrange: ein RUNNING- und ein SUSPENDED-Thread
        PyThreadInfo running = mock(PyThreadInfo.class);
        when(running.getName()).thenReturn("T-run");
        when(running.getState()).thenReturn(PyThreadInfo.State.RUNNING);

        PyThreadInfo suspended = mock(PyThreadInfo.class);
        when(suspended.getName()).thenReturn("T-susp");
        when(suspended.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);

        PyStackFrame f1 = mock(PyStackFrame.class);
        when(f1.getName()).thenReturn("f1");
        PyStackFrame f2 = mock(PyStackFrame.class);
        when(f2.getName()).thenReturn("f2");

        Map<PyThreadInfo, List<PyStackFrame>> framesPerThread = new LinkedHashMap<>();
        framesPerThread.put(running, Collections.emptyList());
        framesPerThread.put(suspended, Arrays.asList(f1, f2));

        try (MockedStatic<DebuggerUtils> debuggerUtilsMock = mockStatic(DebuggerUtils.class);
             MockedStatic<de.code14.edupydebugger.diagram.ObjectDiagramParser> odpMock = mockStatic(de.code14.edupydebugger.diagram.ObjectDiagramParser.class);
             MockedStatic<de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator> pumlMock = mockStatic(de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator.class);
             MockedStatic<DebugServerEndpoint> endpoint = mockStatic(DebugServerEndpoint.class);
             MockedConstruction<de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer> analyzerCons =
                     mockConstruction(de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer.class,
                             (mockAnalyzer, ctx) -> {
                                 // Keine echte Analyse
                                 doNothing().when(mockAnalyzer).analyzeFrames();

                                 // Minimale Daten für handleVariables()
                                 Map<String, List<String>> vars = new LinkedHashMap<>();
                                 // id, names, type, value, scope
                                 vars.put("1", Arrays.asList("x###y", "int", "42", "local"));
                                 when(mockAnalyzer.getVariables()).thenReturn(vars);

                                 // Minimale Daten für handleObjects()
                                 when(mockAnalyzer.getObjects()).thenReturn(Collections.emptyMap());
                             })) {

            // Frames liefern
            debuggerUtilsMock.when(() -> DebuggerUtils.getStackFramesPerThread(mockXDebugSession))
                    .thenReturn(framesPerThread);

            // Objektkarten/Diagramm minimal stubben
            odpMock.when(() -> de.code14.edupydebugger.diagram.ObjectDiagramParser.generateObjectCards(anyMap()))
                    .thenReturn(Collections.singletonMap("1", "puml"));
            odpMock.when(() -> de.code14.edupydebugger.diagram.ObjectDiagramParser.generateObjectDiagram(anyMap()))
                    .thenReturn("puml");
            pumlMock.when(() -> de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator.generateDiagramAsBase64(anyString()))
                    .thenReturn("b64");

            // Act
            sut.performDynamicAnalysis(null);

            // Assert
            // Wichtig: KEIN Callstack bei impliziter Auswahl!
            endpoint.verify(() -> DebugServerEndpoint.publishCallstack(any()), times(0));

            // Variablen + Objektkarten + Objektdiagramm werden publiziert
            endpoint.verify(() -> DebugServerEndpoint.publishVariables(any(VariablesPayload.class)), times(1));
            endpoint.verify(() -> DebugServerEndpoint.publishObjectCards(any(ObjectCardPayload.class)), times(1));
            endpoint.verify(() -> DebugServerEndpoint.publishObjectDiagram(eq("b64")), times(1));
        }
    }
}