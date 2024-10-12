package de.code14.edupydebugger.server;

import com.intellij.execution.process.ProcessHandler;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.ConsoleController;
import de.code14.edupydebugger.core.DebugProcessController;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 0.1.0
 * @since 0.1.0
 */
public class DebugServerEndpointTests {

    @Mock
    private PyDebugProcess mockDebugProcess;
    @Mock
    private Session mockSession;
    @Mock
    private RemoteEndpoint.Basic mockBasicRemote;
    @Mock
    private com.intellij.xdebugger.XDebugSession mockXDebugSession;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        resetWebSocketState();
    }

    private void resetWebSocketState() throws Exception {
        // Reset sessions field using reflection
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);
        sessions.clear();

        // Reset isConnected field using reflection
        Field isConnectedField = DebugServerEndpoint.class.getDeclaredField("isConnected");
        isConnectedField.setAccessible(true);
        isConnectedField.setBoolean(null, false);
    }

    @Test
    public void testOnOpenAddsSession() throws Exception {
        DebugServerEndpoint endpoint = new DebugServerEndpoint();
        endpoint.onOpen(mockSession);

        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertTrue("Session should be added on open.", sessions.contains(mockSession));
        assertTrue("WebSocket should be marked as connected.", getIsConnected());
    }

    @Test
    public void testOnCloseRemovesSession() throws Exception {
        DebugServerEndpoint endpoint = new DebugServerEndpoint();
        endpoint.onOpen(mockSession);
        endpoint.onClose(mockSession);

        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertFalse("Session should be removed on close.", sessions.contains(mockSession));
        assertFalse("WebSocket should be marked as disconnected.", getIsConnected());
    }

    @Test
    public void testSendDebugInfoWhenNotConnected() throws Exception {
        DebugServerEndpoint.sendDebugInfo("Test message");

        Field messageQueueField = DebugServerEndpoint.class.getDeclaredField("messageQueue");
        messageQueueField.setAccessible(true);
        assertFalse("WebSocket should not be connected.", getIsConnected());
    }

    @Test
    public void testSendDebugInfoWhenConnected() throws Exception {
        setIsConnected(true);

        DebugServerEndpoint endpoint = new DebugServerEndpoint();
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote);
        doNothing().when(mockBasicRemote).sendText(anyString());

        // Simuliere das Öffnen der Verbindung
        endpoint.onOpen(mockSession);

        // Teste das Senden der Debug-Nachricht
        DebugServerEndpoint.sendDebugInfo("Test message");

        // Überprüfe, dass die Nachricht zweimal gesendet wurde (einmal bei onOpen und einmal bei sendDebugInfo)
        verify(mockBasicRemote, times(2)).sendText("Test message");
    }

    @Test
    public void testHandleActionMessageForConsoleInput() throws Exception {
        // Arrange: Mock ProcessHandler and set it in the ConsoleController
        ProcessHandler mockProcessHandler = mock(ProcessHandler.class);
        DebugServerEndpoint.setProcessHandler(mockProcessHandler);

        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockProcessHandler.getProcessInput()).thenReturn(mockOutputStream);

        DebugServerEndpoint endpoint = new DebugServerEndpoint();

        // Act: Simulate sending a console-input message
        String inputMessage = "action:console-input:Test Input";
        endpoint.onMessage(inputMessage, mockSession);

        // Assert: Verify that the input was written to the process input stream
        verify(mockOutputStream, times(1)).write(("Test Input\n").getBytes());
        verify(mockOutputStream, times(1)).flush();
    }


    @Test
    public void testHandleActionMessageForResume() {
        when(mockDebugProcess.getSession()).thenReturn(mockXDebugSession);
        DebugServerEndpoint.setDebugProcess(mockDebugProcess);
        DebugServerEndpoint endpoint = new DebugServerEndpoint();

        endpoint.onMessage("action:resume", mockSession);

        verify(mockXDebugSession, times(1)).resume();
    }

    @Test
    public void testSetDebugProcess() throws Exception {
        DebugServerEndpoint.setDebugProcess(mockDebugProcess);

        Field debugProcessControllerField = DebugServerEndpoint.class.getDeclaredField("debugProcessController");
        debugProcessControllerField.setAccessible(true);
        DebugProcessController debugProcessController = (DebugProcessController) debugProcessControllerField.get(null);

        assertEquals(mockDebugProcess, debugProcessController.getDebugProcess());
    }

    @Test
    public void testSetProcessHandler() throws Exception {
        ProcessHandler mockProcessHandler = mock(ProcessHandler.class);

        // Act: Call the setProcessHandler method
        DebugServerEndpoint.setProcessHandler(mockProcessHandler);

        // Use reflection to verify that the processHandler is correctly set in ConsoleController
        Field consoleControllerField = DebugServerEndpoint.class.getDeclaredField("consoleController");
        consoleControllerField.setAccessible(true);
        ConsoleController consoleController = (ConsoleController) consoleControllerField.get(null);

        // Assert that the process handler was correctly passed to ConsoleController
        Field processHandlerField = ConsoleController.class.getDeclaredField("processHandler");
        processHandlerField.setAccessible(true);
        ProcessHandler actualProcessHandler = (ProcessHandler) processHandlerField.get(consoleController);

        assertEquals(mockProcessHandler, actualProcessHandler);
    }

    @Test
    public void testSendObjectCardPlantUmlImagesData() throws Exception {
        setIsConnected(true);

        DebugServerEndpoint endpoint = new DebugServerEndpoint();
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote);
        doNothing().when(mockBasicRemote).sendText(anyString());

        // Simuliere das Öffnen der Verbindung
        endpoint.onOpen(mockSession);

        // Simuliere das Setzen der Object Cards Daten
        String objectCardPlantUmlData = "mockObjectCardData";
        DebugServerEndpoint.setObjectCardPlantUmlImagesData(objectCardPlantUmlData);

        // Teste das Senden der Object Cards Daten
        DebugServerEndpoint.sendDebugInfo("oc:" + objectCardPlantUmlData);

        // Überprüfe, dass die Nachricht gesendet wurde
        verify(mockBasicRemote, times(1)).sendText("oc:mockObjectCardData");
    }

    private boolean getIsConnected() throws Exception {
        Field isConnectedField = DebugServerEndpoint.class.getDeclaredField("isConnected");
        isConnectedField.setAccessible(true);
        return isConnectedField.getBoolean(null);
    }

    private void setIsConnected(boolean isConnected) throws Exception {
        Field isConnectedField = DebugServerEndpoint.class.getDeclaredField("isConnected");
        isConnectedField.setAccessible(true);
        isConnectedField.setBoolean(null, isConnected);
    }

}
