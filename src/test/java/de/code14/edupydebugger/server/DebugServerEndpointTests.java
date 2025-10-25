package de.code14.edupydebugger.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.process.ProcessHandler;
import com.jetbrains.python.debugger.PyDebugProcess;
import de.code14.edupydebugger.core.ConsoleController;
import de.code14.edupydebugger.core.DebugProcessController;
import de.code14.edupydebugger.server.dto.CallstackPayload;
import de.code14.edupydebugger.server.dto.DebugMessage;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DebugServerEndpointTests {

    @Mock private PyDebugProcess mockDebugProcess;
    @Mock private Session mockSession;
    @Mock private RemoteEndpoint.Basic mockBasicRemote;

    private final Gson gson = new Gson();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        resetStaticState();
    }

    /* -------------------- Helpers -------------------- */

    private void resetStaticState() throws Exception {
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);
        sessions.clear();

        Field messageQueueField = DebugServerEndpoint.class.getDeclaredField("messageQueue");
        messageQueueField.setAccessible(true);
        @SuppressWarnings("unchecked")
        BlockingQueue<String> queue = (BlockingQueue<String>) messageQueueField.get(null);
        queue.clear();

        setStatic("isConnected", false);
        setStatic("selectedThread", null);

        setStatic("lastClassDiagram", null);
        setStatic("lastObjectCards", null);
        setStatic("lastObjectDiagram", null);
        setStatic("lastVariables", null);
        setStatic("lastCallstack", null);
        setStatic("lastThreads", null);
    }

    private void setStatic(String name, Object value) throws Exception {
        Field f = DebugServerEndpoint.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private boolean isConnected() throws Exception {
        Field f = DebugServerEndpoint.class.getDeclaredField("isConnected");
        f.setAccessible(true);
        return f.getBoolean(null);
    }

    private BlockingQueue<String> getQueue() throws Exception {
        Field f = DebugServerEndpoint.class.getDeclaredField("messageQueue");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        BlockingQueue<String> q = (BlockingQueue<String>) f.get(null);
        return q;
    }

    /* -------------------- Tests -------------------- */

    @Test
    public void testOnOpenAddsSession_andSetsConnected() throws Exception {
        DebugServerEndpoint ep = new DebugServerEndpoint();
        ep.onOpen(mockSession);

        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertTrue(sessions.contains(mockSession));
        assertTrue(isConnected());
    }

    @Test
    public void testOnCloseRemovesSession_andClearsConnectedWhenEmpty() throws Exception {
        DebugServerEndpoint ep = new DebugServerEndpoint();
        ep.onOpen(mockSession);
        ep.onClose(mockSession);

        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Session> sessions = (Set<Session>) sessionsField.get(null);

        assertFalse(sessions.contains(mockSession));
        assertFalse(isConnected());
    }

    @Test
    public void testSendDebugMessage_whenNotConnected_isQueued() throws Exception {
        DebugServerEndpoint.sendDebugMessage("ping", Collections.singletonMap("x", 1));
        assertFalse(isConnected());
        assertEquals(1, getQueue().size());
    }

    @Test
    public void testSendDebugMessage_whenConnected_isSentAsJson() throws Exception {
        DebugServerEndpoint ep = new DebugServerEndpoint();
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote);
        ep.onOpen(mockSession); // sets isConnected=true

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockBasicRemote).sendText(cap.capture());

        Map<String, Object> payload = new HashMap<>();
        payload.put("a", "b");
        DebugServerEndpoint.sendDebugMessage("test", payload);

        verify(mockBasicRemote, times(1)).sendText(anyString());

        String sent = cap.getValue();
        Type t = new TypeToken<DebugMessage<Map<String, Object>>>(){}.getType();
        DebugMessage<Map<String, Object>> msg = gson.fromJson(sent, t);

        assertEquals("test", msg.type);
        assertEquals("b", msg.payload.get("a"));
    }

    @Test
    public void testConsoleInput_writesToProcessInput() throws Exception {
        // ProcessHandler + OutputStream mocken
        ProcessHandler ph = mock(ProcessHandler.class);
        OutputStream os = mock(OutputStream.class);
        when(ph.getProcessInput()).thenReturn(os);

        // über die öffentliche API setzen
        DebugServerEndpoint.setProcessHandler(ph);

        DebugServerEndpoint ep = new DebugServerEndpoint();
        ep.onOpen(mockSession);

        // JSON „console_input“
        String json = "{\"type\":\"console_input\",\"payload\":{\"text\":\"Hello\"}}";
        ep.onMessage(json, mockSession);

        verify(os, times(1)).write(("Hello\n").getBytes());
        verify(os, times(1)).flush();
    }

    @Test
    public void testAction_resume_callsUnderlyingXDebugSession() {
        // Arrange
        var mockXDebugSession = mock(com.intellij.xdebugger.XDebugSession.class);
        when(mockDebugProcess.getSession()).thenReturn(mockXDebugSession);
        DebugServerEndpoint.setDebugProcess(mockDebugProcess);

        DebugServerEndpoint ep = new DebugServerEndpoint();
        ep.onOpen(mockSession);

        // Act
        String json = "{\"type\":\"action\",\"payload\":{\"command\":\"resume\"}}";
        ep.onMessage(json, mockSession);

        // Assert
        verify(mockXDebugSession, times(1)).resume();
    }

    @Test
    public void testSetDebugProcess_setsIntoController() throws Exception {
        DebugServerEndpoint.setDebugProcess(mockDebugProcess);

        Field f = DebugServerEndpoint.class.getDeclaredField("debugProcessController");
        f.setAccessible(true);
        DebugProcessController controller = (DebugProcessController) f.get(null);

        // Der Controller sollte denselben DebugProcess halten (Getter vorausgesetzt)
        assertEquals(mockDebugProcess, controller.getDebugProcess());
    }

    @Test
    public void testSetProcessHandler_setsIntoConsoleController() throws Exception {
        ProcessHandler ph = mock(ProcessHandler.class);
        DebugServerEndpoint.setProcessHandler(ph);

        Field f = DebugServerEndpoint.class.getDeclaredField("consoleController");
        f.setAccessible(true);
        ConsoleController cc = (ConsoleController) f.get(null);

        Field pf = ConsoleController.class.getDeclaredField("processHandler");
        pf.setAccessible(true);
        assertEquals(ph, pf.get(cc));
    }

    @Test
    public void testGet_callstack_sendsCachedPayloadAsJson() throws Exception {
        // Cached Callstack setzen
        CallstackPayload cached = new CallstackPayload();
        cached.frames = Arrays.asList("f1", "f2");
        setStatic("lastCallstack", cached);

        DebugServerEndpoint ep = new DebugServerEndpoint();
        when(mockSession.getBasicRemote()).thenReturn(mockBasicRemote);
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        doNothing().when(mockBasicRemote).sendText(cap.capture());

        ep.onOpen(mockSession);

        // JSON „get: callstack“
        String json = "{\"type\":\"get\",\"payload\":{\"resource\":\"callstack\"}}";
        ep.onMessage(json, mockSession);

        // Eine Nachricht erwartet
        verify(mockBasicRemote, times(1)).sendText(anyString());

        // zurückparsen
        Type t = new TypeToken<DebugMessage<CallstackPayload>>(){}.getType();
        DebugMessage<CallstackPayload> msg = gson.fromJson(cap.getValue(), t);

        assertEquals("callstack", msg.type);
        assertNotNull(msg.payload);
        assertEquals(Arrays.asList("f1", "f2"), msg.payload.frames);
    }
}