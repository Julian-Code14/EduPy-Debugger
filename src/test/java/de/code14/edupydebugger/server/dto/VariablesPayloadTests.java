package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class VariablesPayloadTests {

    @Test
    public void testPrimitiveVariableRoundtrip() {
        ValueDTO val = new ValueDTO();
        val.kind = "primitive";
        val.repr = "10";

        VariableDTO v = new VariableDTO();
        v.id = "42";
        v.names = Collections.singletonList("x");
        v.pyType = "int";
        v.scope = "local";
        v.value = val;

        VariablesPayload p = new VariablesPayload(Collections.singletonList(v));

        assertEquals(1, p.variables.size());
        VariableDTO got = p.variables.get(0);
        assertEquals("42", got.id);
        assertEquals(Collections.singletonList("x"), got.names);
        assertEquals("int", got.pyType);
        assertEquals("local", got.scope);
        assertNotNull(got.value);
        assertEquals("primitive", got.value.kind);
        assertEquals("10", got.value.repr);
    }

    @Test
    public void testCompositeVariableRoundtrip() {
        ValueDTO val = new ValueDTO();
        val.kind = "composite";
        val.repr = "a: 1\nb: 2";

        VariableDTO v = new VariableDTO();
        v.id = "1337";
        v.names = Arrays.asList("rucksack", "bag");
        v.pyType = "Rucksack";
        v.scope = "instance";
        v.value = val;

        VariablesPayload p = new VariablesPayload(Collections.singletonList(v));

        assertEquals(1, p.variables.size());
        VariableDTO got = p.variables.get(0);
        assertEquals("1337", got.id);
        assertEquals(Arrays.asList("rucksack", "bag"), got.names);
        assertEquals("Rucksack", got.pyType);
        assertEquals("instance", got.scope);
        assertEquals("composite", got.value.kind);
        assertTrue(got.value.repr.contains("a: 1"));
    }
}

