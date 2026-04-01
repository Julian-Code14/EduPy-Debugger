package de.code14.edupydebugger.core;

import com.google.gson.Gson;
import de.code14.edupydebugger.analysis.dynamicanalysis.AttributeInfo;
import de.code14.edupydebugger.analysis.dynamicanalysis.ObjectInfo;
import de.code14.edupydebugger.server.dto.ValueDTO;
import de.code14.edupydebugger.server.dto.VariableDTO;

import java.util.*;

/**
 * Parses the REPL JSON snapshot (variables + objects) into a normalized snapshot.
 */
public final class ReplSnapshotAdapter {
    private ReplSnapshotAdapter() {}

    public static NormalizedSnapshot fromJson(String json) {
        Object parsed = new Gson().fromJson(json, Object.class);

        List<?> varsList = null;
        Map<?,?> objectsMap = null;
        if (parsed instanceof List) {
            varsList = (List<?>) parsed;
        } else if (parsed instanceof Map) {
            Map<?,?> root = (Map<?,?>) parsed;
            Object v = root.get("variables");
            if (v instanceof List) varsList = (List<?>) v;
            Object o = root.get("objects");
            if (o instanceof Map) objectsMap = (Map<?,?>) o;
        }

        List<VariableDTO> varDtos = new ArrayList<>();
        if (varsList != null) {
            for (Object o : varsList) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String,Object> m = (Map<String,Object>) o;
                VariableDTO dto = new VariableDTO();
                dto.id = String.valueOf(m.get("id"));
                dto.names = Collections.singletonList(String.valueOf(m.get("name")));
                dto.pyType = String.valueOf(m.get("type"));
                dto.scope = String.valueOf(m.getOrDefault("scope", "global"));
                ValueDTO val = new ValueDTO();
                val.repr = String.valueOf(m.get("repr"));
                dto.value = val;
                varDtos.add(dto);
            }
        }

        Map<String,ObjectInfo> objects = new HashMap<>();
        if (objectsMap != null) {
            for (Map.Entry<?,?> e : objectsMap.entrySet()) {
                String id = String.valueOf(e.getKey());
                Object val = e.getValue();
                if (!(val instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String,Object> om = (Map<String,Object>) val;
                String ref = String.valueOf(om.get("ref"));
                List<AttributeInfo> attrs = new ArrayList<>();
                Object attrsVal = om.get("attrs");
                if (attrsVal instanceof List) {
                    for (Object a : (List<?>) attrsVal) {
                        if (!(a instanceof Map)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String,Object> am = (Map<String,Object>) a;
                        String an = String.valueOf(am.get("name"));
                        String at = String.valueOf(am.get("type"));
                        String av = String.valueOf(am.get("value"));
                        attrs.add(new AttributeInfo(an, at, av, "public"));
                    }
                }
                List<String> refs = new ArrayList<>();
                refs.add(ref);
                objects.put(id, new ObjectInfo(refs, attrs));
            }
        }

        return new NormalizedSnapshot(varDtos, objects);
    }
}

