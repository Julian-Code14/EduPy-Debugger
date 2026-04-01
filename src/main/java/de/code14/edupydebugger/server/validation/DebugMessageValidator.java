package de.code14.edupydebugger.server.validation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.code14.edupydebugger.server.dto.ConsolePayload;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Centralizes light-weight validation and extraction for inbound WebSocket payloads.
 *
 * The goal is maintainability and log hygiene without changing observable behavior
 * of the debugger. Unknown/invalid inputs are ignored gracefully.
 */
public final class DebugMessageValidator {

    private DebugMessageValidator() {}

    public static Optional<String> extractActionCommand(Object payload, Gson gson) {
        if (payload == null) return Optional.empty();
        String cmd = null;
        if (payload instanceof Map) {
            Object v = ((Map<?, ?>) payload).get("command");
            cmd = v == null ? null : String.valueOf(v);
        } else {
            // Fallback: parse via GSON (keeps prior behavior where possible)
            JsonObject obj = safeObject(payload, gson);
            if (obj != null && obj.has("command")) {
                JsonElement el = obj.get("command");
                cmd = el.isJsonNull() ? null : el.getAsString();
            }
        }
        if (cmd == null || cmd.isBlank() || Objects.equals(cmd, "null")) return Optional.empty();
        return Optional.of(cmd);
    }

    public static Optional<ConsolePayload> extractConsoleInput(Object payload, Gson gson) {
        if (payload == null) return Optional.empty();
        try {
            // Mirror original: serialize payload then re-parse into ConsolePayload
            ConsolePayload p = gson.fromJson(gson.toJson(payload), ConsolePayload.class);
            return Optional.ofNullable(p);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    public static String extractSelectedThread(Object payload, Gson gson) {
        if (payload == null) return null;
        String name = null;
        if (payload instanceof Map) {
            Object v = ((Map<?, ?>) payload).get("name");
            name = v == null ? null : (String) v;
        } else {
            JsonObject obj = safeObject(payload, gson);
            if (obj != null && obj.has("name")) {
                JsonElement el = obj.get("name");
                name = el.isJsonNull() ? null : el.getAsString();
            }
        }
        if (name == null || name.isBlank()) return null;
        return name;
    }

    public static Optional<String> extractGetResource(Object payload, Gson gson) {
        if (payload == null) return Optional.empty();
        String res = null;
        if (payload instanceof Map) {
            Object v = ((Map<?, ?>) payload).get("resource");
            res = v == null ? null : String.valueOf(v);
        } else {
            JsonObject obj = safeObject(payload, gson);
            if (obj != null && obj.has("resource")) {
                JsonElement el = obj.get("resource");
                res = el.isJsonNull() ? null : el.getAsString();
            }
        }
        if (res == null || res.isBlank() || Objects.equals(res, "null")) return Optional.empty();
        return Optional.of(res);
    }

    private static JsonObject safeObject(Object payload, Gson gson) {
        try {
            return gson.fromJson(gson.toJson(payload), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }
}

