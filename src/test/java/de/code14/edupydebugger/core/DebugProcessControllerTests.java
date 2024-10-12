package de.code14.edupydebugger.core;

import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugProcess;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebugProcessControllerTests {

    @Mock
    private PyDebugProcess mockDebugProcess;

    @Mock
    private XDebugSession mockSession;

    private DebugProcessController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDebugProcess.getSession()).thenReturn(mockSession);
        controller = new DebugProcessController();
        controller.setDebugProcess(mockDebugProcess);
    }

    @Test
    public void testResume() {
        // Act
        controller.resume();

        // Assert
        verify(mockSession, times(1)).resume();
    }

    @Test
    public void testPause() {
        // Act
        controller.pause();

        // Assert
        verify(mockSession, times(1)).pause();
    }

    @Test
    public void testStepOver() {
        // Act
        controller.stepOver();

        // Assert
        verify(mockSession, times(1)).stepOver(false);
    }

    @Test
    public void testStepInto() {
        // Act
        controller.stepInto();

        // Assert
        verify(mockSession, times(1)).stepInto();
    }

    @Test
    public void testStepOut() {
        // Act
        controller.stepOut();

        // Assert
        verify(mockSession, times(1)).stepOut();
    }

    @Test
    public void testGetDebugProcess() {
        // Act
        PyDebugProcess returnedProcess = controller.getDebugProcess();

        // Assert
        assertEquals(mockDebugProcess, returnedProcess);
    }

    @Test
    public void testResumeWithNullDebugProcess() {
        // Arrange
        controller.setDebugProcess(null);

        // Act
        controller.resume();

        // Assert
        verify(mockSession, never()).resume(); // Should not interact with session
    }

    @Test
    public void testPauseWithNullDebugProcess() {
        // Arrange
        controller.setDebugProcess(null);

        // Act
        controller.pause();

        // Assert
        verify(mockSession, never()).pause(); // Should not interact with session
    }

    @Test
    public void testStepOverWithNullDebugProcess() {
        // Arrange
        controller.setDebugProcess(null);

        // Act
        controller.stepOver();

        // Assert
        verify(mockSession, never()).stepOver(anyBoolean()); // Should not interact with session
    }

    @Test
    public void testStepIntoWithNullDebugProcess() {
        // Arrange
        controller.setDebugProcess(null);

        // Act
        controller.stepInto();

        // Assert
        verify(mockSession, never()).stepInto(); // Should not interact with session
    }

    @Test
    public void testStepOutWithNullDebugProcess() {
        // Arrange
        controller.setDebugProcess(null);

        // Act
        controller.stepOut();

        // Assert
        verify(mockSession, never()).stepOut(); // Should not interact with session
    }
}

