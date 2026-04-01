package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class DebugMessageTests {

    @Test
    public void testTypedPayload() {
        VariableDTO v = new VariableDTO();
        v.id = "1";
        v.names = Collections.singletonList("x");
        v.pyType = "int";
        v.scope = "local";
        ValueDTO val = new ValueDTO();
        val.kind = "primitive";
        val.repr = "1";
        v.value = val;

        VariablesPayload p = new VariablesPayload(Collections.singletonList(v));
        DebugMessage<VariablesPayload> msg = new DebugMessage<>("variables", p);

        assertEquals("variables", msg.type);
        assertNotNull(msg.payload);
        assertEquals(1, msg.payload.variables.size());
        assertEquals("x", msg.payload.variables.get(0).names.get(0));
    }

    @Test
    public void testGenericMapPayload() {
        DebugMessage<Map<String, Object>> msg = new DebugMessage<>("console", Collections.singletonMap("x", 1));
        assertEquals("console", msg.type);
        assertEquals(1, msg.payload.get("x"));
    }
}

