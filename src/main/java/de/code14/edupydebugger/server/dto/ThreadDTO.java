package de.code14.edupydebugger.server.dto;


/**
 * Represents a single debugger thread and its current execution state.
 */
public class ThreadDTO {

    /** The human-readable thread name (e.g. "MainThread"). */
    public String name;
    /** The thread state ("RUNNING", "SUSPENDED", "WAITING", "KILLED", ...). */
    public String state; // RUNNING|SUSPENDED|KILLED|WAITING

}
