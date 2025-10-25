package de.code14.edupydebugger.server.dto;


/**
 * JSON payload for console output or user input messages.
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "console",
 *   "payload": {
 *     "text": ">>> print('Hello World')"
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * <ul>
 *   <li>Server → Client: forwards live console output</li>
 *   <li>Client → Server: sends user input to the running Python process</li>
 * </ul>
 */
public class ConsolePayload {

    /** Raw text of the console output or user input line. */
    public String text;

}
