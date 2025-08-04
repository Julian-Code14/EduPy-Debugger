package de.code14.edupydebugger.core;

import com.intellij.execution.process.ProcessHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


public class ConsoleControllerTests {

    @Mock
    private ProcessHandler mockProcessHandler;

    @Mock
    private OutputStream mockOutputStream;

    private ConsoleController consoleController;

    @Before
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Mock process handler behavior to return an output stream
        when(mockProcessHandler.getProcessInput()).thenReturn(mockOutputStream);

        // Initialize the ConsoleController with the mock process handler
        consoleController = new ConsoleController();
        consoleController.setProcessHandler(mockProcessHandler);
    }

    @Test
    public void testSendInputToProcess() throws IOException {
        // Arrange
        String input = "test input";

        // Act
        consoleController.sendInputToProcess(input);

        // Assert: Ensure input is written to the process with a newline and flushed
        verify(mockOutputStream, times(1)).write((input + "\n").getBytes());
        verify(mockOutputStream, times(1)).flush();
    }

    @Test(expected = IOException.class)
    public void testSendInputToProcessThrowsIOException() throws IOException {
        // Arrange
        String input = "test input";

        // Simulate an IOException when writing to the output stream
        doThrow(new IOException()).when(mockOutputStream).write(any(byte[].class));

        // Act & Assert: Expect IOException when calling the method
        consoleController.sendInputToProcess(input);
    }

    @Test(expected = NullPointerException.class)
    public void testSendInputToProcessWithNullProcessHandler() throws IOException {
        // Arrange
        consoleController.setProcessHandler(null);

        // Act & Assert: Expect NullPointerException when processHandler is null
        consoleController.sendInputToProcess("test input");
    }

    @Test(expected = NullPointerException.class)
    public void testSendInputToProcessWithNullStream() throws IOException {
        // Arrange
        when(mockProcessHandler.getProcessInput()).thenReturn(null);

        // Act & Assert: Expect NullPointerException if the stream is null
        consoleController.sendInputToProcess("test input");
    }

    @Test
    public void testSetProcessHandler() {
        // Arrange: create a new mock ProcessHandler
        ProcessHandler newProcessHandler = mock(ProcessHandler.class);

        // Act: Set the new ProcessHandler
        consoleController.setProcessHandler(newProcessHandler);

        // Assert: Ensure the ProcessHandler was set correctly
        assertSame(newProcessHandler, consoleController.getProcessHandler());
    }

}
