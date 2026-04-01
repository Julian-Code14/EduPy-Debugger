package de.code14.edupydebugger.server;

import de.code14.edupydebugger.core.repl.ReplManager;
import jakarta.websocket.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for REPL-specific behaviors in DebugServerEndpoint: requesting snapshots
 * and resetting the REPL.
 */
public class DebugServerEndpointReplTests {

    @Before
    public void setUp() throws Exception {
        resetStaticState();
    }

    private void resetStaticState() throws Exception {
        Field sessionsField = DebugServerEndpoint.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<Session> sessions = (java.util.Set<Session>) sessionsField.get(null);
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

    @Test
    public void testGetVariablesRequestsReplSnapshot() {
        DebugServerEndpoint ep = new DebugServerEndpoint();
        try (MockedStatic<ReplManager> replStatic = Mockito.mockStatic(ReplManager.class)) {
            ReplManager rm = mock(ReplManager.class);
            replStatic.when(ReplManager::getInstance).thenReturn(rm);

            // Simuliere: kein DebugProcess aktiv → REPL-Path
            // und bereits ein beliebiger ProcessHandler gesetzt (damit ensureConsoleTarget nicht NEU startet)
            DebugServerEndpoint.setProcessHandler(mock(com.intellij.execution.process.ProcessHandler.class));

            String json = "{\"type\":\"get\",\"payload\":{\"resource\":\"variables\"}}";
            ep.onMessage(json, null);

            // requestSnapshot sollte aufgerufen worden sein
            verify(rm, atLeastOnce()).requestSnapshot();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testReplResetStopsProcessAndClearsCache() throws Exception {
        DebugServerEndpoint ep = new DebugServerEndpoint();
        try (MockedStatic<ReplManager> replStatic = Mockito.mockStatic(ReplManager.class)) {
            ReplManager rm = mock(ReplManager.class);
            replStatic.when(ReplManager::getInstance).thenReturn(rm);

            String json = "{\"type\":\"repl_reset\",\"payload\":{}}";
            ep.onMessage(json, null);

            verify(rm, times(1)).stopRepl();

            // Prüfen, dass Caches leer sind
            Field f = DebugServerEndpoint.class.getDeclaredField("lastVariables");
            f.setAccessible(true);
            assertNull(f.get(null));
        }
    }
}
