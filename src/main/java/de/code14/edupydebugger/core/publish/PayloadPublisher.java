package de.code14.edupydebugger.core.publish;

import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.diagram.ObjectDiagramParser;
import de.code14.edupydebugger.diagram.PlantUMLDiagramGenerator;
import de.code14.edupydebugger.server.DebugServerEndpoint;
import de.code14.edupydebugger.server.dto.*;

import java.io.IOException;
import java.util.*;

/** Central publishing for variables/object cards/diagram (used by REPL and Debug). */
public final class PayloadPublisher {
    private PayloadPublisher() {}

    public static void publishVariablesWithSnippet(List<VariableDTO> variables, Map<String, ObjectInfo> objects) {
        Set<String> prim = new HashSet<>(Arrays.asList("int","float","str","bool","list","dict","tuple","set"));
        Set<String> containers = new HashSet<>(Arrays.asList("list","dict","tuple","set"));
        for (VariableDTO dto : variables) {
            if (dto == null || dto.value == null) continue;
            String t = dto.pyType != null ? dto.pyType : "";
            if (!prim.contains(t)) {
                ObjectInfo info = (objects != null) ? objects.get(dto.id) : null;
                StringBuilder sb = new StringBuilder();
                StringBuilder full = new StringBuilder();
                if (info != null) {
                    for (AttributeInfo a : info.attributes()) {
                        String shown = a.value();
                        if (shown.length() > 20) shown = shown.substring(0, 20) + " [...]";
                        sb.append(a.name()).append(": ").append(shown).append("\n");
                        full.append(a.name()).append(": ").append(a.value()).append("\n");
                    }
                }
                dto.value.kind = "composite";
                dto.value.repr = sb.toString().trim();
                dto.value.full = full.toString().trim();
            } else {
                // Primitive & builtin containers
                dto.value.kind = "primitive";
                String base = dto.value.repr != null ? dto.value.repr.replace("~", ", ") : "";
                if (containers.contains(t)) {
                    // Supply full and a shortened preview
                    dto.value.full = (dto.value.full != null && !dto.value.full.isEmpty()) ? dto.value.full : base;
                    String full = dto.value.full; // set above to non-null
                    dto.value.repr = (full.length() > 60) ? (full.substring(0, 60) + " [...]") : base;
                } else {
                    // Non-container primitives keep full=null and show base
                    dto.value.repr = base;
                }
            }
        }
        DebugServerEndpoint.publishVariables(new VariablesPayload(variables));
    }

    public static void publishObjects(Map<String, ObjectInfo> objects) throws IOException {
        if (objects == null) objects = java.util.Collections.emptyMap();
        Map<String, String> cardsPuml = ObjectDiagramParser.generateObjectCards(objects);
        ObjectCardPayload ocPayload = new ObjectCardPayload();
        ocPayload.cards = new ArrayList<>();
        for (Map.Entry<String, String> entry : cardsPuml.entrySet()) {
            String base64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(entry.getValue());
            CardDTO c = new CardDTO();
            c.id = entry.getKey();
            c.svgBase64 = base64;
            ocPayload.cards.add(c);
        }
        DebugServerEndpoint.publishObjectCards(ocPayload);

        String odPuml = ObjectDiagramParser.generateObjectDiagram(objects);
        String odBase64 = PlantUMLDiagramGenerator.generateDiagramAsBase64(odPuml);
        DebugServerEndpoint.publishObjectDiagram(odBase64);
    }
}
