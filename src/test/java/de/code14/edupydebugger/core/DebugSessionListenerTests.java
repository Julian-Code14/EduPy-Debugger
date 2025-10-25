package de.code14.edupydebugger.core;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import com.jetbrains.python.debugger.PyThreadInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.DebuggerUtils;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ThreadDTO;
import de.code14.edupydebugger.server.dto.ThreadsPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

// Test-Subclass, die die statische Analyse im Konstruktor unterdrückt.
class TestDebugSessionListener extends DebugSessionListener {
    public TestDebugSessionListener(PyDebugProcess debugProcess) {
        super(debugProcess);
    }
    @Override
    protected void performStaticAnalysis(PyDebugProcess pyDebugProcess) {
        // keine statische Analyse in Tests
    }
}

public class DebugSessionListenerTests {

    @Mock private XDebugSession mockXDebugSession;
    @Mock private PyDebugProcess mockPyDebugProcess;
    @Mock private Project mockProject;

    private DebugSessionListener sut; // System under test

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockPyDebugProcess.getSession()).thenReturn(mockXDebugSession);
        when(mockPyDebugProcess.getProject()).thenReturn(mockProject);
        when(mockProject.getBasePath()).thenReturn("dummyPath");

        // Instanz mit unterdrückter statischer Analyse
        sut = new TestDebugSessionListener(mockPyDebugProcess);
    }

    @Test
    public void testConstructorTriggersStaticAnalysis_butIsNoopInTestSubclass() throws IOException {
        // Static-Mock VOR der Instanziierung aktivieren und DANN eine neue Testinstanz erzeugen.
        try (MockedStatic<DebugServerEndpoint> endpointMock = Mockito.mockStatic(DebugServerEndpoint.class)) {
            DebugSessionListener local = new TestDebugSessionListener(mockPyDebugProcess);
            // Da performStaticAnalysis() in der Test-Subklasse leer ist, erwarten wir keine Publikationen.
            endpointMock.verifyNoInteractions();
            // „local“ nur genutzt, damit der Compiler keine Warnung wirft.
            assertEquals(TestDebugSessionListener.class, local.getClass());
        }
    }

    @Test
    public void testStackFrameChanged_publishesThreadsPayload_andTriggersDynamicAnalysis() throws Exception {
        try (MockedStatic<DebuggerUtils> debuggerUtilsMock = Mockito.mockStatic(DebuggerUtils.class);
             MockedStatic<DebugServerEndpoint> endpoint = Mockito.mockStatic(DebugServerEndpoint.class)) {

            DebugSessionController dummy = mock(DebugSessionController.class);
            endpoint.when(DebugServerEndpoint::getDebugSessionController).thenReturn(dummy);
            endpoint.when(DebugServerEndpoint::getSelectedThread).thenReturn(null);

            PyThreadInfo t1 = mock(PyThreadInfo.class);
            when(t1.getName()).thenReturn("Thread-A");
            when(t1.getState()).thenReturn(PyThreadInfo.State.SUSPENDED);

            debuggerUtilsMock.when(() -> DebuggerUtils.getThreads(mockXDebugSession))
                    .thenReturn(List.of(t1));

            sut.stackFrameChanged();

            // Erwartet: Threads einmal publiziert …
            endpoint.verify(() -> DebugServerEndpoint.publishThreads(argThat(tp ->
                    tp != null && tp.threads != null &&
                            tp.threads.size() == 1 &&
                            "Thread-A".equals(tp.threads.get(0).name)
            )), times(1));

            // … und die Dynamic Analysis angestoßen.
            verify(dummy, times(1)).performDynamicAnalysis(null);

            // Kein verifyNoInteractions mehr – es gibt legitime weitere Aufrufe.
        }
    }
}