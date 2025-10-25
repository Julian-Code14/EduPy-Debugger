package de.code14.edupydebugger.server.dto;


import java.util.List;

/**
 * JSON payload containing a list of rendered object cards
 * (each one encoded as a Base64 PlantUML SVG).
 *
 * <h3>Schema (JSON)</h3>
 * <pre>{@code
 * {
 *   "type": "object_cards",
 *   "payload": {
 *     "cards": [
 *       { "id": "1337", "svgBase64": "PHN2ZyB4bWxucz0..." },
 *       { "id": "1338", "svgBase64": "PHN2ZyB4bWxucz0..." }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * Server → Client
 */
public class ObjectCardPayload {

    /** List of individual object cards (one per instance). */
    public List<CardDTO> cards;

}
