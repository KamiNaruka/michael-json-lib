package heehee.michael.json.patch;

import heehee.michael.json.Json;
import heehee.michael.json.JsonNull;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.Map;
import java.util.Objects;

/**
 * An RFC 7396 JSON Merge Patch: a JSON value describing changes to apply to a target document.
 *
 * <p>Unlike {@link JsonPatch}, a merge patch is itself just JSON &mdash; object members set to
 * {@code null} mean "remove this member", object members present with a non-null value are
 * merged recursively (if both sides are objects) or replace the target value outright, and a
 * patch that isn't an object replaces the whole target.
 *
 * <p><b>RFC 7396 limitation:</b> because {@code null} means "remove", a merge patch has no
 * way to represent "set this member to JSON {@code null}". {@link #diff} fails fast with
 * {@link IllegalArgumentException} when such a transformation is requested, rather than
 * returning a patch with different semantics. Use {@link JsonPatch} for that case.
 */
public final class JsonMergePatch {

    private final JsonValue patch;

    private JsonMergePatch(JsonValue patch) {
        this.patch = JsonCopy.deepCopy(patch);
    }

    /**
     * Creates a merge patch from a JSON value, taking a deep copy so later mutations of the argument do not affect this patch.
     *
     * @param patch merge-patch document
     * @return immutable merge-patch wrapper
     */
    public static JsonMergePatch of(JsonValue patch) {
        return new JsonMergePatch(Objects.requireNonNull(patch, "patch"));
    }

    /**
     * Parses JSON text as an RFC 7396 merge patch.
     *
     * @param json merge-patch JSON text
     * @return parsed merge patch
     */
    public static JsonMergePatch fromJson(String json) {
        return of(Json.parse(json));
    }

    /** The raw patch document. */
    public JsonValue toJson() {
        return JsonCopy.deepCopy(patch);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return patch.toPrettyString();
    }

    /** Applies this merge patch to {@code target}, returning the resulting document (input is left untouched). */
    public JsonValue apply(JsonValue target) {
        return merge(JsonCopy.deepCopy(target), patch);
    }

    private static JsonValue merge(JsonValue target, JsonValue patch) {
        if (patch.isObject()) {
            JsonObject result = target.isObject() ? target.asObject() : new JsonObject();
            for (Map.Entry<String, JsonValue> e : patch.asObject()) {
                String name = e.getKey();
                JsonValue value = e.getValue();
                if (value.isNull()) {
                    result.remove(name);
                } else {
                    JsonValue targetChild = result.containsKey(name) ? result.get(name) : JsonNull.INSTANCE;
                    result.put(name, merge(targetChild, value));
                }
            }
            return result;
        }
        // A non-object patch (including this recursive call's leaf case) replaces wholesale.
        return JsonCopy.deepCopy(patch);
    }

    // ------------------------------------------------------------------
    // diff
    // ------------------------------------------------------------------

    /**
     * Produces a merge patch that transforms {@code source} into {@code target}. Only object
     * members are diffed recursively; arrays (and any other non-object value) that differ at
     * all are replaced wholesale, matching what {@link #apply} can actually express.
     */
    public static JsonMergePatch diff(JsonValue source, JsonValue target) {
        return new JsonMergePatch(diffValue(source, target));
    }

    private static JsonValue diffValue(JsonValue source, JsonValue target) {
        if (source.isObject() && target.isObject()) {
            JsonObject patch = new JsonObject();
            JsonObject s = source.asObject();
            JsonObject t = target.asObject();
            for (String key : s.keySet()) {
                if (!t.containsKey(key)) patch.put(key, JsonNull.INSTANCE);
            }
            for (Map.Entry<String, JsonValue> e : t) {
                String key = e.getKey();
                JsonValue tv = e.getValue();
                if (!s.containsKey(key)) {
                    if (tv.isNull()) throw unrepresentableNull(key);
                    patch.put(key, JsonCopy.deepCopy(tv));
                } else {
                    JsonValue sv = s.get(key);
                    if (!sv.equals(tv)) {
                        if (tv.isNull()) throw unrepresentableNull(key);
                        patch.put(key, diffValue(sv, tv));
                    }
                }
            }
            return patch;
        }
        // Not both objects: a top-level (or type-changed) scalar/array diff is just "replace
        // wholesale with target" -- there's no partial-merge representation for it.
        return JsonCopy.deepCopy(target);
    }

    private static IllegalArgumentException unrepresentableNull(String key) {
        return new IllegalArgumentException(
                "RFC 7396 merge patch cannot represent setting object member '" + key + "' to JSON null; use JsonPatch instead");
    }
}
