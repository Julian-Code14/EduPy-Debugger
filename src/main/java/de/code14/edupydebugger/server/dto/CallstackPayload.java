package de.code14.edupydebugger.server.dto;


import java.util.List;

/**
 * JSON payload representing the current Python call stack.
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "callstack",
 *   "payload": {
 *     "frames": ["main()", "solve_knapsack()", "add_item()"]
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * Server → Client
 */
public class CallstackPayload {

    /** Names of stack frames in top-down order. */
    public List<String> frames;

}
