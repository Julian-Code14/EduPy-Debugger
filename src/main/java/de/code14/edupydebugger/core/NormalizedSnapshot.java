package de.code14.edupydebugger.core;

import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.server.dto.VariableDTO;

import java.util.List;
import java.util.Map;

/**
 * Normalized view of variables and objects for publishing to the frontend,
 * independent of the data source (Debugger vs. REPL).
 */
public record NormalizedSnapshot(List<VariableDTO> variables,
                                 Map<String, ObjectInfo> objects) {}

