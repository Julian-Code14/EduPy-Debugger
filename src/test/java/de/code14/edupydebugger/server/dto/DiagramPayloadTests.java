package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiagramPayloadTests {

    @Test
    public void testCtorAndFieldAssignment() {
        DiagramPayload p = new DiagramPayload();
        assertNull(p.svgBase64);

        p.svgBase64 = "PHN2ZyAvPg==";
        assertEquals("PHN2ZyAvPg==", p.svgBase64);
    }
}

