package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ObjectCardPayloadTests {

    @Test
    public void testObjectCardPayloadRoundtrip() {
        CardDTO c1 = new CardDTO();
        c1.id = "1";
        c1.svgBase64 = "AAA";
        CardDTO c2 = new CardDTO();
        c2.id = "2";
        c2.svgBase64 = "BBB";

        ObjectCardPayload p = new ObjectCardPayload();
        p.cards = Arrays.asList(c1, c2);

        assertEquals(2, p.cards.size());
        assertEquals("1", p.cards.get(0).id);
        assertEquals("BBB", p.cards.get(1).svgBase64);
    }
}

