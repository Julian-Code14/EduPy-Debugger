package de.code14.edupydebugger.server.dto;


/**
 * Generic wrapper object representing a typed JSON message exchanged between
 * the EduPy Debugger backend and the frontend.
 * <p>
 * Each message consists of a {@code type} string that identifies the logical
 * communication channel (for example {@code "variables"}, {@code "threads"},
 * {@code "console"}), and a {@code payload} object that carries the message data.
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "variables",
 *   "payload": {
 *     "variables": [
 *       {
 *         "id": "42",
 *         "names": ["x"],
 *         "pyType": "int",
 *         "scope": "local",
 *         "value": {
 *           "kind": "primitive",
 *           "repr": "10"
 *         }
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * <ul>
 *   <li>Server → Client: published updates (class diagram, variables, call stack, console output, ...)</li>
 *   <li>Client → Server: control commands or requests (actions, console input, thread selection, ...)</li>
 * </ul>
 *
 * @param <T> the Java type of the JSON payload (e.g. {@link VariablesPayload},
 *            {@link ThreadsPayload}, or {@link ConsolePayload})
 */
public class DebugMessage<T> {

    /** Logical message type (channel name). */
    public String type;

    /** The data payload associated with this message. */
    public T payload;

    /**
     * Constructs a new typed debug message.
     *
     * @param type    the logical message type
     * @param payload the message payload (arbitrary serializable object)
     */
    public DebugMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
    }

}
