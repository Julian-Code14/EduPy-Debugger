package de.code14.edupydebugger.core;

import de.code14.edupydebugger.core.console.ConsoleOutputListener;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for student-friendly formatting of Python tracebacks in ConsoleOutputListener.
 */
public class ConsoleOutputListenerTracebackTests {

    @Test
    public void formatsZeroDivisionErrorIntoFriendlyMessage() throws Exception {
        var listener = new ConsoleOutputListener(new DummyProcessHandler());

        try (MockedStatic<DebugServerEndpoint> mocked = Mockito.mockStatic(DebugServerEndpoint.class)) {
            ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);

            mocked.when(() -> DebugServerEndpoint.sendDebugMessage(any(String.class), any()))
                    .then(inv -> null);

            // Full traceback in one chunk
            String tb = String.join("\n",
                    "Traceback (most recent call last):",
                    "  File \"main.py\", line 3, in <module>",
                    "    print(1/0)",
                    "ZeroDivisionError: division by zero",
                    "");

            listener.attachConsoleListeners();
            DummyProcessHandler dph = (DummyProcessHandler) listenerProcessHandler(listener);
            var pl = dph.getCaptured();
            pl.onTextAvailable(new TestProcessEvent(tb), new com.intellij.openapi.util.Key<Object>("stderr"){});

            mocked.verify(() -> DebugServerEndpoint.sendDebugMessage(typeCap.capture(), payloadCap.capture()));
            assertEquals("console", typeCap.getValue());
            assertTrue(payloadCap.getValue() instanceof ConsolePayload);
            String text = ((ConsolePayload) payloadCap.getValue()).text;

            assertTrue(text.contains("Fehler erkannt: ZeroDivisionError"));
            assertTrue(text.contains("Ort: main.py:3"));
            assertTrue(text.contains("Tipp:"));
            assertTrue(text.contains("Python: ZeroDivisionError: division by zero"));
        }
    }

    @Test
    public void buffersUntilTracebackCompleteThenEmitsOnce() throws Exception {
        var listener = new ConsoleOutputListener(new DummyProcessHandler());

        try (MockedStatic<DebugServerEndpoint> mocked = Mockito.mockStatic(DebugServerEndpoint.class)) {
            mocked.when(() -> DebugServerEndpoint.sendDebugMessage(any(String.class), any()))
                    .then(inv -> null);

            listener.attachConsoleListeners();
            DummyProcessHandler dph = (DummyProcessHandler) listenerProcessHandler(listener);
            var pl = dph.getCaptured();

            // Send the first two lines only (should NOT emit yet)
            pl.onTextAvailable(new TestProcessEvent("Traceback (most recent call last):\n"), new com.intellij.openapi.util.Key<Object>("stderr"){});
            pl.onTextAvailable(new TestProcessEvent("  File \"main.py\", line 10, in <module>\n"), new com.intellij.openapi.util.Key<Object>("stderr"){});
            mocked.verify(() -> DebugServerEndpoint.sendDebugMessage(eq("console"), any()), Mockito.never());

            // Now send the last line with the exception type → exactly one emit
            pl.onTextAvailable(new TestProcessEvent("IndentationError: unexpected indent\n"), new com.intellij.openapi.util.Key<Object>("stderr"){});

            ArgumentCaptor<Object> payloadCap = ArgumentCaptor.forClass(Object.class);
            mocked.verify(() -> DebugServerEndpoint.sendDebugMessage(eq("console"), payloadCap.capture()), Mockito.times(1));
            String text = ((ConsolePayload) payloadCap.getValue()).text;
            assertTrue(text.contains("Fehler erkannt: IndentationError"));
            assertTrue(text.contains("Ort: main.py:10"));
            assertTrue(text.contains("Tabulator-Taste"));
        }
    }

    // Helpers copied from snapshot tests
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

    private static Object listenerProcessHandler(ConsoleOutputListener l) throws Exception {
        var f = ConsoleOutputListener.class.getDeclaredField("processHandler");
        f.setAccessible(true);
        return f.get(l);
    }
}

