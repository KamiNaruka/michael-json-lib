package heehee.michael.json.patch;

import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.Locale;

/** A single RFC 6902 JSON Patch operation ({@code add}, {@code remove}, {@code replace}, {@code move}, {@code copy}, or {@code test}). */
public final class JsonPatchOperation {

    public enum Op {
        ADD, REMOVE, REPLACE, MOVE, COPY, TEST;

        String wireName() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Op fromWireName(String s) {
            for (Op o : values()) {
                if (o.wireName().equals(s)) return o;
            }
            throw new JsonPatchException("Unknown JSON Patch operation: \"" + s + "\"");
        }
    }

    private final Op op;
    private final JsonPointer path;
    private final JsonPointer from;
    private final JsonValue value;

    private JsonPatchOperation(Op op, JsonPointer path, JsonPointer from, JsonValue value) {
        this.op = op;
        this.path = path;
        this.from = from;
        this.value = value == null ? null : JsonCopy.deepCopy(value);
    }

    public static JsonPatchOperation add(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.ADD, require(path, "path"), null, require(value, "value"));
    }

    public static JsonPatchOperation remove(JsonPointer path) {
        return new JsonPatchOperation(Op.REMOVE, require(path, "path"), null, null);
    }

    public static JsonPatchOperation replace(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.REPLACE, require(path, "path"), null, require(value, "value"));
    }

    public static JsonPatchOperation move(JsonPointer from, JsonPointer path) {
        return new JsonPatchOperation(Op.MOVE, require(path, "path"), require(from, "from"), null);
    }

    public static JsonPatchOperation copy(JsonPointer from, JsonPointer path) {
        return new JsonPatchOperation(Op.COPY, require(path, "path"), require(from, "from"), null);
    }

    public static JsonPatchOperation test(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.TEST, require(path, "path"), null, require(value, "value"));
    }

    private static <T> T require(T v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " must not be null");
        return v;
    }

    public Op op() { return op; }
    public JsonPointer path() { return path; }
    public JsonPointer from() { return from; }
    public JsonValue value() { return value == null ? null : JsonCopy.deepCopy(value); }
    JsonValue valueInternal() { return value; }

    /** Serializes this operation to its wire form, e.g. {@code {"op":"add","path":"/a","value":1}}. */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.put("op", op.wireName());
        obj.put("path", path.toString());
        if (from != null) obj.put("from", from.toString());
        if (value != null) obj.put("value", JsonCopy.deepCopy(value));
        return obj;
    }

    /** Parses one operation from its wire form. */
    public static JsonPatchOperation fromJson(JsonObject json) {
        if (!json.containsKey("op") || !json.containsKey("path")) {
            throw new JsonPatchException("A patch operation needs 'op' and 'path' members: " + json);
        }
        Op op = Op.fromWireName(json.getString("op"));
        JsonPointer path = JsonPointer.parse(json.getString("path"));
        switch (op) {
            case ADD:
            case REPLACE:
            case TEST:
                if (!json.containsKey("value")) {
                    throw new JsonPatchException("'" + op.wireName() + "' requires a 'value' member: " + json);
                }
                return new JsonPatchOperation(op, path, null, json.get("value"));
            case REMOVE:
                return new JsonPatchOperation(op, path, null, null);
            case MOVE:
            case COPY:
                if (!json.containsKey("from")) {
                    throw new JsonPatchException("'" + op.wireName() + "' requires a 'from' member: " + json);
                }
                return new JsonPatchOperation(op, path, JsonPointer.parse(json.getString("from")), null);
            default:
                throw new AssertionError("Unreachable: " + op);
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
