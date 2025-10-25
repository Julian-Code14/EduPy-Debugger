package de.code14.edupydebugger.server.dto;


/**
 * Represents the value portion of a variable, either primitive or composite.
 */
public class ValueDTO {

    /** "primitive" or "composite" */
    public String kind; // "primitive" | "ref" | "composite"
    /** String representation or summarized attribute list */
    public String repr; // bei primitive/composite

}
