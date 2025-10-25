package de.code14.edupydebugger.server.dto;


/**
 * Generic JSON payload for any PlantUML-based diagram represented as a Base64-encoded SVG.
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "class_diagram" | "object_diagram",
 *   "payload": {
 *     "svgBase64": "PHN2ZyB4bWxucz0..."
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * Server → Client
 */
public class DiagramPayload {

    /** The Base64-encoded SVG content of the diagram. */
    public String svgBase64;

}
