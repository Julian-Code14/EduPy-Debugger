package de.code14.edupydebugger.server.dto;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ThreadsPayloadTests {

    @Test
    public void testThreadsPayloadRoundtrip() {
        ThreadDTO t1 = new ThreadDTO();
        t1.name = "MainThread";
        t1.state = "SUSPENDED";

        ThreadDTO t2 = new ThreadDTO();
        t2.name = "Worker-1";
        t2.state = "RUNNING";

        ThreadsPayload p = new ThreadsPayload();
        p.threads = Arrays.asList(t1, t2);

        assertEquals(2, p.threads.size());
        assertEquals("MainThread", p.threads.get(0).name);
        assertEquals("SUSPENDED", p.threads.get(0).state);
        assertEquals("Worker-1", p.threads.get(1).name);
        assertEquals("RUNNING", p.threads.get(1).state);
    }
}

