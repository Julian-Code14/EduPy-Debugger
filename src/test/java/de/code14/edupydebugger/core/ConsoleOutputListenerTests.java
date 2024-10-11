package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 11.10.24
 */
public class ConsoleOutputListenerTests {

    @Mock private ProcessHandler processHandler;  // Mocked ProcessHandler
    @Mock private ProcessEvent processEvent;      // Mocked ProcessEvent
    @Mock private Key outputType;                 // Mocked OutputType (stdout, stderr, etc.)

    private ConsoleOutputListener consoleOutputListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Create instance of ConsoleOutputListener with the mocked ProcessHandler
        consoleOutputListener = new ConsoleOutputListener(processHandler);
    }

    @Test
    public void testAttachConsoleListeners_whenProcessHandlerIsNotNull() {
        // Act: Call the method that attaches listeners
        consoleOutputListener.attachConsoleListeners();

        // Capture the listener that was added to the processHandler
        ArgumentCaptor<ProcessListener> listenerCaptor = ArgumentCaptor.forClass(ProcessListener.class);
        verify(processHandler).addProcessListener(listenerCaptor.capture());

        // Simulate a process event with sample text
        ProcessListener capturedListener = listenerCaptor.getValue();
        when(processEvent.getText()).thenReturn("Sample output");
        capturedListener.onTextAvailable(processEvent, outputType);

        // Verify that the event text was captured and used
        verify(processEvent, times(1)).getText();
    }

    @Test
    public void testAttachConsoleListeners_whenProcessHandlerIsNull() {
        // Arrange: Create a new ConsoleOutputListener with a null ProcessHandler
        ConsoleOutputListener consoleOutputListenerWithNullHandler = new ConsoleOutputListener(null);

        // Act & Assert: Ensure that IllegalStateException is thrown
        assertThrows(IllegalStateException.class, consoleOutputListenerWithNullHandler::attachConsoleListeners);
    }

}
