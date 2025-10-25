package de.code14.edupydebugger.server.dto;


import java.util.List;

/**
 * JSON payload containing all active debugger threads.
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "threads",
 *   "payload": {
 *     "threads": [
 *       { "name": "MainThread", "state": "SUSPENDED" },
 *       { "name": "Worker-1", "state": "RUNNING" }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * Server → Client
 */
public class ThreadsPayload {

    /** List of all currently known threads in the debugger session. */
    public List<ThreadDTO> threads;

}
