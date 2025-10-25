package de.code14.edupydebugger.server.dto;


import java.util.List;

/**
 * JSON payload representing the current set of visible variables
 * in the selected stack frame.
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
 *       },
 *       {
 *         "id": "1337",
 *         "names": ["rucksack"],
 *         "pyType": "Rucksack",
 *         "scope": "instance",
 *         "value": {
 *           "kind": "composite",
 *           "repr": "inhalt: 5\\ngewicht: 10"
 *         }
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 *
 * <h3>Direction</h3>
 * Server → Client
 */
public class VariablesPayload {

    /** List of variable descriptors. */
    public List<VariableDTO> variables;

    public VariablesPayload(List<VariableDTO> variables) {
        this.variables = variables;
    }

}
