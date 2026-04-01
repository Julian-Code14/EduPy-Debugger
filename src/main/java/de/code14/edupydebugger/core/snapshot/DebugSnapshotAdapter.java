package de.code14.edupydebugger.core.snapshot;

import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.StackFrameAnalyzer;
import de.code14.edupydebugger.server.dto.ValueDTO;
import de.code14.edupydebugger.server.dto.VariableDTO;

import java.util.*;

/** Builds a normalized snapshot (variables + objects) from a StackFrameAnalyzer (debug path). */
public final class DebugSnapshotAdapter {
    private DebugSnapshotAdapter() {}

    private static final Set<String> PRIMS = new HashSet<>(Arrays.asList(
            "int","float","str","bool","list","dict","tuple","set"));

    public static NormalizedSnapshot from(StackFrameAnalyzer analyzer) {
        Map<String, List<String>> varsRaw = analyzer.getVariables();
        Map<String, ObjectInfo> objects = analyzer.getObjects();

        List<VariableDTO> variables = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : varsRaw.entrySet()) {
            List<String> v = e.getValue();
            if (v.size() < 4) continue;
            String id    = e.getKey();
            String names = v.get(0);
            String type  = v.get(1);
            String value = v.get(2);
            String scope = v.get(3);

            VariableDTO dto = new VariableDTO();
            dto.id = id;
            dto.names = Arrays.asList(names.split("###"));
            dto.pyType = type;
            dto.scope = scope;

            ValueDTO val = new ValueDTO();
            if (PRIMS.contains(type)) {
                val.kind = "primitive";
                val.repr = value.replace("~", ", ");
            } else {
                val.kind = "composite";
                val.repr = value; // snippet will be rebuilt centrally in PayloadPublisher
            }
            dto.value = val;
            variables.add(dto);
        }

        return new NormalizedSnapshot(variables, objects);
    }
}

