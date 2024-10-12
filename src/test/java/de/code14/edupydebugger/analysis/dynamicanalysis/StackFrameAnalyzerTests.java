package de.code14.edupydebugger.analysis.dynamicanalysis;

import com.jetbrains.python.debugger.PyStackFrame;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
 */
public class StackFrameAnalyzerTests {

    private StackFrameAnalyzer stackFrameAnalyzer;
    private VariableAnalyzer mockVariableAnalyzer;
    private ObjectAnalyzer mockObjectAnalyzer;

    @Before
    public void setUp() {
        List<PyStackFrame> pyStackFrames = Collections.emptyList();  // Leere Liste von PyStackFrames

        // Mocks erstellen
        mockVariableAnalyzer = mock(VariableAnalyzer.class);
        mockObjectAnalyzer = mock(ObjectAnalyzer.class);

        // Erstellen eines echten StackFrameAnalyzer-Objekts
        stackFrameAnalyzer = new StackFrameAnalyzer(pyStackFrames);

        // Die Mocks in die echte Instanz injizieren
        injectMocks(stackFrameAnalyzer, mockVariableAnalyzer, mockObjectAnalyzer);
    }

    private void injectMocks(StackFrameAnalyzer analyzer, VariableAnalyzer varAnalyzer, ObjectAnalyzer objAnalyzer) {
        try {
            var variableAnalyzerField = StackFrameAnalyzer.class.getDeclaredField("variableAnalyzer");
            variableAnalyzerField.setAccessible(true);
            variableAnalyzerField.set(analyzer, varAnalyzer);

            var objectAnalyzerField = StackFrameAnalyzer.class.getDeclaredField("objectAnalyzer");
            objectAnalyzerField.setAccessible(true);
            objectAnalyzerField.set(analyzer, objAnalyzer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testAnalyzeFrames() {
        // Ausführen der Methode
        stackFrameAnalyzer.analyzeFrames();

        // Überprüfen, ob die Methoden der Abhängigkeiten aufgerufen wurden
        verify(mockVariableAnalyzer, times(1)).analyzeVariables();
        verify(mockObjectAnalyzer, times(1)).analyzeObjects();
    }

    @Test
    public void testGetVariables() {
        // Ausführen der Methode
        Map<String, List<String>> variables = stackFrameAnalyzer.getVariables();

        // Sicherstellen, dass die Variablen nicht null sind
        assertNotNull(variables);
        verify(mockVariableAnalyzer, times(1)).getVariables();
    }

    @Test
    public void testGetObjects() {
        // Ausführen der Methode
        Map<String, ObjectInfo> objects = stackFrameAnalyzer.getObjects();

        // Sicherstellen, dass die Objekte nicht null sind
        assertNotNull(objects);
        verify(mockObjectAnalyzer, times(1)).getObjects();
    }
}

