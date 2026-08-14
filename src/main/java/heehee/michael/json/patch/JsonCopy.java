package heehee.michael.json.patch;

import heehee.michael.json.JsonArray;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Internal defensive deep-copy helper for mutable JSON containers. */
final class JsonCopy {

    private static final int MAX_DEPTH = 512;

    private JsonCopy() { }

    static JsonValue deepCopy(JsonValue v) {
        if (v == null) throw new IllegalArgumentException("JSON value must not be null");
        return deepCopy(v, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    private static JsonValue deepCopy(JsonValue v, Set<JsonValue> active, int depth) {
        if (!v.isObject() && !v.isArray()) return v;
        if (depth >= MAX_DEPTH) throw new IllegalArgumentException("JSON nesting exceeds maximum depth of " + MAX_DEPTH);
        if (!active.add(v)) throw new IllegalArgumentException("Cyclic JsonValue graph is not valid JSON");
        try {
            if (v.isObject()) {
                JsonObject copy = new JsonObject();
                for (Map.Entry<String, JsonValue> e : v.asObject()) {
                    copy.put(e.getKey(), deepCopy(e.getValue(), active, depth + 1));
                }
                return copy;
            }
            JsonArray copy = new JsonArray();
            for (JsonValue e : v.asArray()) copy.add(deepCopy(e, active, depth + 1));
            return copy;
        } finally {
            active.remove(v);
        }
    }
}
