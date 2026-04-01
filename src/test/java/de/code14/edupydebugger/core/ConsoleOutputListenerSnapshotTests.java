package de.code14.edupydebugger.core;

import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ObjectCardPayload;
import de.code14.edupydebugger.server.dto.VariablesPayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Verifies that ConsoleOutputListener parses REPL snapshots and publishes
 * variables as well as object cards/diagram.
 */
public class ConsoleOutputListenerSnapshotTests {

    @Before
    public void setUp() { }

    @Test
    public void testSnapshotPublishesVariablesAndObjects() throws Exception {
        var listener = new ConsoleOutputListener(new DummyProcessHandler());

        try (MockedStatic<DebugServerEndpoint> mocked = Mockito.mockStatic(DebugServerEndpoint.class)) {
            ArgumentCaptor<VariablesPayload> varsCap = ArgumentCaptor.forClass(VariablesPayload.class);
            ArgumentCaptor<ObjectCardPayload> cardsCap = ArgumentCaptor.forClass(ObjectCardPayload.class);

            mocked.when(() -> DebugServerEndpoint.publishVariables(any(VariablesPayload.class)))
                    .then(inv -> null);
            mocked.when(() -> DebugServerEndpoint.publishObjectCards(any(ObjectCardPayload.class)))
                    .then(inv -> null);
            mocked.when(() -> DebugServerEndpoint.publishObjectDiagram(Mockito.anyString()))
                    .then(inv -> null);

            // Simuliere eine vollständige Snapshot-Zeile (Variables + Objects)
            String json = "{" +
                    "\"variables\":[{" +
                    "\"id\":\"1\",\"name\":\"a\",\"type\":\"Apfel\",\"repr\":\"Apfel()\",\"scope\":\"global\"}]," +
                    "\"objects\":{\"1\":{\"ref\":\"a: Apfel\",\"attrs\":[{" +
                    "\"name\":\"farbe\",\"type\":\"str\",\"value\":\"'rot'\",\"visibility\":\"public\"}]}}}";
            String line = "__EDUPY_SNAPSHOT__" + json + "\n";

            // Direkt die private Pufferlogik umgehen, indem wir publishVariablesFromSnapshot triggern
            // über die Text-Event Strecke
            var event = new TestProcessEvent(line);
            listener.attachConsoleListeners();
            DummyProcessHandler dph = (DummyProcessHandler) listenerProcessHandler(listener);
            com.intellij.execution.process.ProcessListener pl = dph.getCaptured();
            pl.onTextAvailable(event, new com.intellij.openapi.util.Key<Object>("stdout"){});

            mocked.verify(() -> DebugServerEndpoint.publishVariables(varsCap.capture()), Mockito.times(1));
            mocked.verify(() -> DebugServerEndpoint.publishObjectCards(cardsCap.capture()), Mockito.times(1));
            mocked.verify(() -> DebugServerEndpoint.publishObjectDiagram(Mockito.anyString()), Mockito.times(1));

            VariablesPayload vp = varsCap.getValue();
            assertNotNull(vp);
            assertFalse(vp.variables.isEmpty());
            assertEquals("a", vp.variables.get(0).names.get(0));
            assertEquals("Apfel", vp.variables.get(0).pyType);

            ObjectCardPayload oc = cardsCap.getValue();
            assertNotNull(oc);
            assertFalse(oc.cards.isEmpty());
        }
    }

    // Helpers
    private static class DummyProcessHandler extends com.intellij.execution.process.ProcessHandler {
        private com.intellij.execution.process.ProcessListener captured;
        @Override protected void destroyProcessImpl() {}
        @Override protected void detachProcessImpl() {}
        @Override public boolean detachIsDefault() { return false; }
        @Override public java.io.OutputStream getProcessInput() { return null; }
        @Override public void addProcessListener(com.intellij.execution.process.ProcessListener listener) { this.captured = listener; }
        public com.intellij.execution.process.ProcessListener getCaptured(){ return captured; }
    }

    private static class TestProcessEvent extends com.intellij.execution.process.ProcessEvent {
        private final String text;
        public TestProcessEvent(String text) { super(new DummyProcessHandler(), text); this.text = text; }
        @Override public String getText() { return text; }
    }

    // Hack zum Zugriff auf das private Feld via Reflexion
    private static Object listenerProcessHandler(ConsoleOutputListener l) throws Exception {
        var f = ConsoleOutputListener.class.getDeclaredField("processHandler");
        f.setAccessible(true);
        return f.get(l);
    }
}
