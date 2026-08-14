package heehee.michael.json.patch;

import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.Locale;

/** A single RFC 6902 JSON Patch operation ({@code add}, {@code remove}, {@code replace}, {@code move}, {@code copy}, or {@code test}). */
public final class JsonPatchOperation {

    /**
     * The six operation kinds defined by RFC 6902.
     */
    public enum Op {
        /** Adds a value at the target path. */
        ADD,
        /** Removes the value at the target path. */
        REMOVE,
        /** Replaces the value at the target path. */
        REPLACE,
        /** Moves a value from a source path to the target path. */
        MOVE,
        /** Copies a value from a source path to the target path. */
        COPY,
        /** Verifies that the target value equals the supplied value. */
        TEST;

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

    /**
     * Creates an {@code add} operation. The supplied value is deep-copied into the operation.
     *
     * @param path destination pointer
     * @param value value to add
     * @return add operation
     */
    public static JsonPatchOperation add(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.ADD, require(path, "path"), null, require(value, "value"));
    }

    /**
     * Creates a {@code remove} operation.
     *
     * @param path pointer to the value to remove
     * @return remove operation
     */
    public static JsonPatchOperation remove(JsonPointer path) {
        return new JsonPatchOperation(Op.REMOVE, require(path, "path"), null, null);
    }

    /**
     * Creates a {@code replace} operation. The supplied value is deep-copied into the operation.
     *
     * @param path pointer to the value to replace
     * @param value replacement value
     * @return replace operation
     */
    public static JsonPatchOperation replace(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.REPLACE, require(path, "path"), null, require(value, "value"));
    }

    /**
     * Creates a {@code move} operation.
     *
     * @param from source pointer
     * @param path destination pointer
     * @return move operation
     */
    public static JsonPatchOperation move(JsonPointer from, JsonPointer path) {
        return new JsonPatchOperation(Op.MOVE, require(path, "path"), require(from, "from"), null);
    }

    /**
     * Creates a {@code copy} operation.
     *
     * @param from source pointer
     * @param path destination pointer
     * @return copy operation
     */
    public static JsonPatchOperation copy(JsonPointer from, JsonPointer path) {
        return new JsonPatchOperation(Op.COPY, require(path, "path"), require(from, "from"), null);
    }

    /**
     * Creates a {@code test} operation. The supplied comparison value is deep-copied into the operation.
     *
     * @param path pointer to test
     * @param value expected value
     * @return test operation
     */
    public static JsonPatchOperation test(JsonPointer path, JsonValue value) {
        return new JsonPatchOperation(Op.TEST, require(path, "path"), null, require(value, "value"));
    }

    private static <T> T require(T v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " must not be null");
        return v;
    }

    /**
     * Returns this operation's kind.
     *
     * @return operation kind
     */
    public Op op() { return op; }
    /**
     * Returns this operation's target path.
     *
     * @return target pointer
     */
    public JsonPointer path() { return path; }
    /**
     * Returns the source pointer used by {@code move} and {@code copy}.
     *
     * @return source pointer, or {@code null} for other operation kinds
     */
    public JsonPointer from() { return from; }
    /**
     * Returns a deep copy of the value used by {@code add}, {@code replace}, or {@code test}.
     *
     * @return copied operation value, or {@code null} for operation kinds without a value
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toJson().toString();
    }
}
