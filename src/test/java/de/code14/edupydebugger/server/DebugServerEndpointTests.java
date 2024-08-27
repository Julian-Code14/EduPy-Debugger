package de.code14.edupydebugger.server;

import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.DebugProcessController;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author julian
 * @version 1.0
 * @since 14.08.24
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
