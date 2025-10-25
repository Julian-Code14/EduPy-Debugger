package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.ConsolePayload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConsoleOutputListenerTests {

    @Mock private ProcessHandler processHandler;
    @Mock private ProcessEvent processEvent;
    @Mock private Key outputType;

    private ConsoleOutputListener consoleOutputListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        consoleOutputListener = new ConsoleOutputListener(processHandler);
    }

    @Test
    public void testAttachConsoleListeners_forwardsConsoleOutputAsJson() {
        // Static-Mocking von DebugServerEndpoint
        try (MockedStatic<DebugServerEndpoint> mockedStatic = Mockito.mockStatic(DebugServerEndpoint.class)) {

            // Wir fangen den zweiten Parameter (payload) ab, um dessen Inhalt zu verifizieren
            AtomicReference<Object> capturedPayload = new AtomicReference<>();
            mockedStatic.when(() ->
                    DebugServerEndpoint.sendDebugMessage(anyString(), any())
            ).then(invocation -> {
                capturedPayload.set(invocation.getArgument(1));
                return null;
            });

            // Act: Listener anhängen und ein Text-Event simulieren
            consoleOutputListener.attachConsoleListeners();

            ArgumentCaptor<ProcessListener> listenerCaptor = ArgumentCaptor.forClass(ProcessListener.class);
            verify(processHandler).addProcessListener(listenerCaptor.capture());

            ProcessListener listener = listenerCaptor.getValue();
            when(processEvent.getText()).thenReturn("Sample console output");
            listener.onTextAvailable(processEvent, outputType);

            // Assert: sendDebugMessage("console", <ConsolePayload>) wurde aufgerufen
            mockedStatic.verify(() ->
                    DebugServerEndpoint.sendDebugMessage(eq("console"), any()), times(1)
            );

            // Payload inhaltlich prüfen
            Object payload = capturedPayload.get();
            assertNotNull("Payload should not be null", payload);
            assertTrue("Payload should be a ConsolePayload", payload instanceof ConsolePayload);
            assertEquals("Sample console output", ((ConsolePayload) payload).text);

            // Sicherstellen, dass der Text vom Event gelesen wurde
            verify(processEvent, times(1)).getText();
        }
    }

    @Test
    public void testAttachConsoleListeners_doesNotThrow() {
        // Erwartung: kein Fehler beim Anhängen der Listener
        consoleOutputListener.attachConsoleListeners();
        // wenn wir hier ankommen, war alles OK
    }
}