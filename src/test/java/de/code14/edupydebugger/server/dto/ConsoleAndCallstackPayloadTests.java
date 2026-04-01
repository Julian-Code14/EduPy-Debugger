package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ConsoleAndCallstackPayloadTests {

    @Test
    public void testConsolePayloadRoundtrip() {
        ConsolePayload p = new ConsolePayload();
        assertNull(p.text);
        p.text = ">>> print('hi')";
        assertEquals(">>> print('hi')", p.text);
    }

    @Test
    public void testCallstackPayloadRoundtrip() {
        CallstackPayload p = new CallstackPayload();
        p.frames = Arrays.asList("main()", "solve_knapsack(n=5)", "helper(x=1)");
        assertEquals(3, p.frames.size());
        assertEquals("main()", p.frames.get(0));
        assertEquals("helper(x=1)", p.frames.get(2));
    }
}
