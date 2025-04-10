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
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for DebugServerEndpoint.
 * This class covers various behaviors including session management,
 * sending messages, and handling of action messages.
 *
 * @author julian
 * @version 0.3.0
 * @since 0.1.0
 */
public class DebugServerEndpointTests {

    @Mock
    private PyDebugProcess mockDebugProcess; // Mock for PyDebugProcess.
    @Mock
    private Session mockSession; // Mock for a WebSocket Session.
    @Mock
    private RemoteEndpoint.Basic mockBasicRemote; // Mock for RemoteEndpoint.Basic.
    @Mock
    private com.intellij.xdebugger.XDebugSession mockXDebugSession; // Mock for XDebugSession.

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this); // Initialize Mockito annotations.

        // Stub mockDebugProcess.getSession() to return the mocked XDebugSession.
        when(mockDebugProcess.getSession()).thenReturn(mockXDebugSession);

        // Reset static state for DebugServerEndpoint.
        resetWebSocketState();
    }

    /**
     * Resets the static fields in DebugServerEndpoint that are not final.
     * For final fields (such as sessions and messageQueue), their contents are cleared.
     */
    private void resetWebSocketState() throws Exception {
        // Clear the 'sessions' set.
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);
        sessions.clear();

        // Clear the 'messageQueue'.
        Field messageQueueField = DebugServerEndpoint.class.getDeclaredField("messageQueue");
        messageQueueField.setAccessible(true);
        BlockingQueue<String> queue = (BlockingQueue<String>) messageQueueField.get(null);
        queue.clear();

        // Reset non-final static fields.
        setStaticNonFinalField("isConnected", false);
        setStaticNonFinalField("selectedThread", null);
        setStaticNonFinalField("callStackString", null);
        setStaticNonFinalField("threadOptionsString", null);
        setStaticNonFinalField("classDiagramPlantUmlImage", null);
        setStaticNonFinalField("variablesString", null);
        setStaticNonFinalField("objectCardPlantUmlImagesData", null);
        setStaticNonFinalField("objectDiagramPlantUmlImage", null);
    }

    /**
     * Sets a non-final static field in DebugServerEndpoint.
     */
    private void setStaticNonFinalField(String fieldName, Object value) throws Exception {
        Field f = DebugServerEndpoint.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Test
    public void testOnOpenAddsSession() throws Exception {
        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create instance of DebugServerEndpoint.
        endpoint.onOpen(mockSession); // Invoke onOpen with mockSession.

        // Retrieve the static 'sessions' field and verify it contains the mockSession.
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertTrue("Session should be added on open.", sessions.contains(mockSession));
        assertTrue("WebSocket should be marked as connected.", isConnected());
    }

    @Test
    public void testOnCloseRemovesSession() throws Exception {
        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.
        endpoint.onOpen(mockSession); // Open connection.
        endpoint.onClose(mockSession); // Close connection.

        // Retrieve the static 'sessions' field and verify the session is removed.
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertFalse("Session should be removed on close.", sessions.contains(mockSession));
        assertFalse("WebSocket should be marked as disconnected.", isConnected());
    }

    @Test
    public void testSendDebugInfoWhenNotConnected() throws Exception {
        // Call sendDebugInfo when not connected; message is queued.
        DebugServerEndpoint.sendDebugInfo("Test message");
        assertFalse("WebSocket should not be connected.", isConnected());
    }

    @Test
    public void testSendDebugInfoWhenConnected() throws Exception {
        // Set the static 'isConnected' flag to true.
        setStaticNonFinalField("isConnected", true);

        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote); // Stub getBasicRemote to return mockBasicRemote.
        doNothing().when(mockBasicRemote).sendText(anyString()); // Stub sendText.

        endpoint.onOpen(mockSession); // Simulate onOpen with mockSession.

        // Call sendDebugInfo; no queued messages exist so one invocation is expected.
        DebugServerEndpoint.sendDebugInfo("Test message");

        verify(mockBasicRemote, times(1)).sendText("Test message"); // Verify sendText was called once.
    }

    @Test
    public void testHandleActionMessageForConsoleInput() throws Exception {
        // Create a mock ProcessHandler.
        ProcessHandler mockProcessHandler = mock(ProcessHandler.class);
        DebugServerEndpoint.setProcessHandler(mockProcessHandler); // Set process handler in DebugServerEndpoint.

        // Stub process input stream.
        OutputStream mockOutputStream = mock(OutputStream.class);
        when(mockProcessHandler.getProcessInput()).thenReturn(mockOutputStream);

        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.

        // Simulate receiving a console input action message.
        String inputMessage = "action:console-input:Test Input";
        endpoint.onMessage(inputMessage, mockSession);

        // Verify that the process input stream received the expected input and was flushed.
        verify(mockOutputStream, times(1)).write(("Test Input\n").getBytes());
        verify(mockOutputStream, times(1)).flush();
    }

    @Test
    public void testHandleActionMessageForResume() {
        // Stub debugProcess.getSession() to return the mocked XDebugSession.
        when(mockDebugProcess.getSession()).thenReturn(mockXDebugSession);
        DebugServerEndpoint.setDebugProcess(mockDebugProcess); // Set debug process in DebugServerEndpoint.
        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.

        endpoint.onMessage("action:resume", mockSession); // Simulate a resume action.

        verify(mockXDebugSession, times(1)).resume(); // Verify resume() is invoked on the XDebugSession.
    }

    @Test
    public void testSetDebugProcess() throws Exception {
        DebugServerEndpoint.setDebugProcess(mockDebugProcess); // Set debug process.

        // Retrieve the static debugProcessController and verify it contains the correct process.
        Field debugProcessControllerField = DebugServerEndpoint.class.getDeclaredField("debugProcessController");
        debugProcessControllerField.setAccessible(true);
        DebugProcessController debugProcessController = (DebugProcessController) debugProcessControllerField.get(null);

        assertEquals(mockDebugProcess, debugProcessController.getDebugProcess());
    }

    @Test
    public void testSetProcessHandler() throws Exception {
        ProcessHandler mockProcessHandler = mock(ProcessHandler.class); // Create a mock ProcessHandler.

        DebugServerEndpoint.setProcessHandler(mockProcessHandler); // Set the process handler.

        // Retrieve the static ConsoleController and verify its processHandler is set correctly.
        Field consoleControllerField = DebugServerEndpoint.class.getDeclaredField("consoleController");
        consoleControllerField.setAccessible(true);
        ConsoleController consoleController = (ConsoleController) consoleControllerField.get(null);

        Field processHandlerField = ConsoleController.class.getDeclaredField("processHandler");
        processHandlerField.setAccessible(true);
        ProcessHandler actualProcessHandler = (ProcessHandler) processHandlerField.get(consoleController);

        assertEquals(mockProcessHandler, actualProcessHandler);
    }

    @Test
    public void testSendObjectCardPlantUmlImagesData() throws Exception {
        // Set the connection flag to true.
        setIsConnected(true);

        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote); // Stub getBasicRemote.
        doNothing().when(mockBasicRemote).sendText(anyString()); // Stub sendText.

        endpoint.onOpen(mockSession); // Simulate opening the connection.

        // Set the object card PlantUML data.
        String objectCardPlantUmlData = "mockObjectCardData";
        DebugServerEndpoint.setObjectCardPlantUmlImagesData(objectCardPlantUmlData);

        // Invoke sendDebugInfo for object card data.
        DebugServerEndpoint.sendDebugInfo("oc:" + objectCardPlantUmlData);

        // Verify that the expected message is sent.
        verify(mockBasicRemote, times(1)).sendText("oc:mockObjectCardData");
    }

    @Test
    public void testHandleGetMessageCallstack() throws Exception {
        // Set the connection flag to true.
        setIsConnected(true);

        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote); // Stub getBasicRemote.
        doNothing().when(mockBasicRemote).sendText(anyString()); // Stub sendText.

        DebugServerEndpoint endpoint = new DebugServerEndpoint(); // Create DebugServerEndpoint instance.
        endpoint.onOpen(mockSession); // Simulate opening the connection.

        // Set the static callStackString to a dummy value.
        setStaticNonFinalField("callStackString", "dummyCallStack");

        // Simulate reception of a "get:callstack" message.
        endpoint.onMessage("get:callstack", mockSession);

        // Verify that the callstack information is sent.
        verify(mockBasicRemote, times(1)).sendText("callstack:dummyCallStack");
    }

    // Helper method to get the static "isConnected" flag.
    private boolean isConnected() throws Exception {
        Field isConnectedField = DebugServerEndpoint.class.getDeclaredField("isConnected");
        isConnectedField.setAccessible(true);
        return isConnectedField.getBoolean(null);
    }

    // Helper method to set the static "isConnected" flag.
    private void setIsConnected(boolean connected) throws Exception {
        Field isConnectedField = DebugServerEndpoint.class.getDeclaredField("isConnected");
        isConnectedField.setAccessible(true);
        isConnectedField.setBoolean(null, connected);
    }
}