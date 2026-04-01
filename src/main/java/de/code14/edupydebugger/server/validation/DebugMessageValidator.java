package de.code14.edupydebugger.server.validation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.code14.edupydebugger.server.dto.ConsolePayload;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Centralizes light‑weight validation and extraction for inbound WebSocket payloads.
 * <p>
 * Responsibility: convert loosely typed JSON payloads into safe, typed values that the
 * endpoint can consume without ad‑hoc casts or {@code NullPointerException}s. Invalid or
 * incomplete inputs are ignored gracefully.
 * <p>
 * Scope: parsing/normalization only. No behavioral decisions (e.g., allowed command names)
 * are enforced here; those remain in the endpoint/controllers to keep responsibilities clear.
 */
public final class DebugMessageValidator {

    private DebugMessageValidator() {}

    /**
     * Extracts the {@code command} field from an {@code action} message payload.
     * <ul>
     *   <li>Accepts map‑like payloads and generic JSON objects.</li>
     *   <li>Returns {@link Optional#empty()} when the field is missing/blank.</li>
     *   <li>Does not validate the command value itself (kept in endpoint logic).</li>
     * </ul>
     */
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

    /**
     * Builds a {@link ConsolePayload} from a {@code console_input} message payload.
     * <p>
     * Mirrors the endpoint’s historical behavior by serializing the generic object and
     * re‑parsing it as {@code ConsolePayload}. Returns empty if parsing fails.
     */
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

    /**
     * Extracts the selected thread name from a {@code thread_selected} payload.
     * <ul>
     *   <li>Blank or missing names are normalized to {@code null}.</li>
     *   <li>No existence check against actual thread list is performed here.</li>
     * </ul>
     */
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

    /**
     * Extracts the {@code resource} field from a {@code get} message payload.
     * <p>
     * Returns {@link Optional#empty()} for missing/blank values. The set of supported
     * resources is validated by the endpoint, not here.
     */
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

    /**
     * Attempts to convert an arbitrary object into a {@link JsonObject} using {@link Gson}.
     * Returns {@code null} if conversion fails.
     */
    private static JsonObject safeObject(Object payload, Gson gson) {
        try {
            return gson.fromJson(gson.toJson(payload), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }
}
