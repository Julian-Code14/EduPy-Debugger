package de.code14.edupydebugger.server.dto;


import java.util.List;

/** Describes a single variable or object reference. */
public class VariableDTO {

    public String id;
    public List<String> names;
    public String pyType;
    public String scope; // local | global | unknown
    public ValueDTO value; // primitive | ref | composite

}
