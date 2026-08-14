package heehee.michael.json.patch;

import heehee.michael.json.Json;
import heehee.michael.json.JsonArray;
import heehee.michael.json.JsonNull;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * An RFC 6902 JSON Patch: an ordered sequence of {@link JsonPatchOperation}s that transform
 * one JSON document into another.
 *
 * <p>{@link #apply(JsonValue)} never mutates its input &mdash; it deep-copies the document
 * before applying operations and returns the (possibly different) resulting root value, since
 * a whole-document operation (e.g. {@code {"op":"replace","path":"","value":...}}) can't be
 * expressed as an in-place mutation of the original reference.
 *
 * <p>{@link #diff(JsonValue, JsonValue)} produces a patch that transforms {@code source} into
 * {@code target}. Object members are diffed by key and array elements positionally (by index,
 * comparing then appending/trimming any length difference) &mdash; this always produces a
 * correct patch, though not necessarily the shortest possible one for reordered/inserted array
 * elements (computing a minimal diff there is an LCS problem this keeps intentionally simple).
 */
public final class JsonPatch implements Iterable<JsonPatchOperation> {

    private final List<JsonPatchOperation> operations;

    private JsonPatch(List<JsonPatchOperation> operations) {
        this.operations = List.copyOf(operations);
    }

    /**
     * Creates a patch from the supplied operations in order.
     *
     * @param operations ordered operations
     * @return patch
     */
    public static JsonPatch of(JsonPatchOperation... operations) {
        return new JsonPatch(Arrays.asList(operations));
    }

    /**
     * Creates a patch from a list of operations in order. The list is copied.
     *
     * @param operations ordered operations
     * @return patch
     */
    public static JsonPatch of(List<JsonPatchOperation> operations) {
        return new JsonPatch(operations);
    }

    /**
     * Parses an RFC 6902 patch from its JSON array representation.
     *
     * @param patchArray patch array
     * @return parsed patch
     * @throws JsonPatchException if an element is not a valid operation object
     */
    public static JsonPatch fromJson(JsonArray patchArray) {
        List<JsonPatchOperation> ops = new ArrayList<>(patchArray.size());
        for (JsonValue v : patchArray) ops.add(JsonPatchOperation.fromJson(v.asObject()));
        return new JsonPatch(ops);
    }

    /**
     * Parses JSON text as an RFC 6902 patch array.
     *
     * @param json patch JSON text
     * @return parsed patch
     */
    public static JsonPatch fromJson(String json) {
        return fromJson(Json.parse(json).asArray());
    }

    /**
     * Serializes the patch to a new JSON array of operation objects.
     *
     * @return patch array
     */
    public JsonArray toJson() {
        JsonArray arr = new JsonArray();
        for (JsonPatchOperation op : operations) arr.add(op.toJson());
        return arr;
    }

    /**
     * Returns the ordered operations as an immutable list.
     *
     * @return immutable operation list
     */
    public List<JsonPatchOperation> operations() {
        return operations;
    }

    /**
     * Returns the number of operations in this patch.
     *
     * @return operation count
     */
    public int size() {
        return operations.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.Iterator<JsonPatchOperation> iterator() {
        return operations.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toJson().toPrettyString();
    }

    // ------------------------------------------------------------------
    // apply
    // ------------------------------------------------------------------

    /**
     * Applies this patch to {@code document} without mutating the supplied document.
     *
     * @param document document to patch
     * @return patched document
     * @throws JsonPatchException if an operation cannot be applied
     */
    public JsonValue apply(JsonValue document) {
        JsonValue root = JsonCopy.deepCopy(document);
        for (JsonPatchOperation op : operations) {
            root = applyOne(root, op);
        }
        return root;
    }

    private static JsonValue applyOne(JsonValue root, JsonPatchOperation op) {
        switch (op.op()) {
            case ADD:
                return applyAdd(root, op.path(), op.valueInternal());
            case REMOVE:
                return applyRemove(root, op.path());
            case REPLACE:
                return applyReplace(root, op.path(), op.valueInternal());
            case TEST: {
                JsonValue actual = resolve(root, op.path(), "test");
                if (!actual.equals(op.valueInternal())) {
                    throw new JsonPatchException(
                            "'test' failed at " + op.path() + ": expected " + op.valueInternal() + " but found " + actual);
                }
                return root;
            }
            case MOVE: {
                if (op.from().isPrefixOf(op.path())) {
                    throw new JsonPatchException("'from' (" + op.from() + ") must not be a proper prefix of 'path' (" + op.path() + ") in a move");
                }
                JsonValue moved = resolve(root, op.from(), "move");
                root = applyRemove(root, op.from());
                return applyAdd(root, op.path(), moved);
            }
            case COPY: {
                JsonValue copied = resolve(root, op.from(), "copy");
                return applyAdd(root, op.path(), copied);
            }
            default:
                throw new AssertionError("Unreachable: " + op.op());
        }
    }

    private static JsonValue applyAdd(JsonValue root, JsonPointer path, JsonValue value) {
        if (path.isRoot()) return JsonCopy.deepCopy(value);
        JsonValue parent = resolve(root, path.parent(), "add");
        String lastToken = path.lastToken();
        if (parent.isObject()) {
            parent.asObject().put(lastToken, JsonCopy.deepCopy(value));
        } else if (parent.isArray()) {
            JsonArray arr = parent.asArray();
            arr.add(parseIndex(lastToken, arr.size(), true), JsonCopy.deepCopy(value));
        } else {
            throw new JsonPatchException("Cannot add into a non-container at " + path.parent());
        }
        return root;
    }

    private static JsonValue applyRemove(JsonValue root, JsonPointer path) {
        if (path.isRoot()) throw new JsonPatchException("Cannot remove the whole document");
        JsonValue parent = resolve(root, path.parent(), "remove");
        String lastToken = path.lastToken();
        if (parent.isObject()) {
            JsonObject obj = parent.asObject();
            if (!obj.containsKey(lastToken)) throw new JsonPatchException("No such member to remove: " + path);
            obj.remove(lastToken);
        } else if (parent.isArray()) {
            JsonArray arr = parent.asArray();
            arr.remove(parseIndex(lastToken, arr.size(), false));
        } else {
            throw new JsonPatchException("Cannot remove from a non-container at " + path.parent());
        }
        return root;
    }

    private static JsonValue applyReplace(JsonValue root, JsonPointer path, JsonValue value) {
        if (path.isRoot()) return JsonCopy.deepCopy(value);
        JsonValue parent = resolve(root, path.parent(), "replace");
        String lastToken = path.lastToken();
        if (parent.isObject()) {
            JsonObject obj = parent.asObject();
            if (!obj.containsKey(lastToken)) throw new JsonPatchException("No such member to replace: " + path);
            obj.put(lastToken, JsonCopy.deepCopy(value));
        } else if (parent.isArray()) {
            JsonArray arr = parent.asArray();
            int idx = parseIndex(lastToken, arr.size(), false);
            arr.remove(idx);
            arr.add(idx, JsonCopy.deepCopy(value));
        } else {
            throw new JsonPatchException("Cannot replace within a non-container at " + path.parent());
        }
        return root;
    }

    private static JsonValue resolve(JsonValue root, JsonPointer pointer, String opName) {
        try {
            return pointer.evaluate(root);
        } catch (JsonPointerException e) {
            throw new JsonPatchException("Cannot '" + opName + "': path does not resolve: " + pointer, e);
        }
    }

    /** Parses a JSON Patch array-index token; {@code "-"} is only legal (as size()) when {@code forInsert}. */
    private static int parseIndex(String token, int size, boolean forInsert) {
        if (token.equals("-")) {
            if (forInsert) return size;
            throw new JsonPatchException("'-' is not a valid index for this operation");
        }
        if (!token.matches("0|[1-9][0-9]*")) {
            throw new JsonPatchException("Invalid array index: \"" + token + "\"");
        }
        final int idx;
        try {
            idx = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new JsonPatchException("Array index is too large: \"" + token + "\"");
        }
        int max = forInsert ? size : size - 1;
        if (idx < 0 || idx > max) {
            throw new JsonPatchException("Array index " + idx + " out of range (size=" + size + ")");
        }
        return idx;
    }

    // ------------------------------------------------------------------
    // diff
    // ------------------------------------------------------------------

    /**
     * Produces a JSON Patch that transforms {@code source} into {@code target}.
     *
     * @param source original document
     * @param target desired document
     * @return patch operations representing the transformation
     */
    public static JsonPatch diff(JsonValue source, JsonValue target) {
        List<JsonPatchOperation> ops = new ArrayList<>();
        diff(JsonPointer.root(), source, target, ops);
        return new JsonPatch(ops);
    }

    private static void diff(JsonPointer at, JsonValue source, JsonValue target, List<JsonPatchOperation> ops) {
        if (source.equals(target)) return;
        if (source.isObject() && target.isObject()) {
            diffObjects(at, source.asObject(), target.asObject(), ops);
        } else if (source.isArray() && target.isArray()) {
            diffArrays(at, source.asArray(), target.asArray(), ops);
        } else {
            ops.add(JsonPatchOperation.replace(at, JsonCopy.deepCopy(target)));
        }
    }

    private static void diffObjects(JsonPointer at, JsonObject source, JsonObject target, List<JsonPatchOperation> ops) {
        for (String key : source.keySet()) {
            if (!target.containsKey(key)) ops.add(JsonPatchOperation.remove(at.child(key)));
        }
        for (Map.Entry<String, JsonValue> e : target) {
            String key = e.getKey();
            JsonValue tv = e.getValue();
            if (!source.containsKey(key)) {
                ops.add(JsonPatchOperation.add(at.child(key), JsonCopy.deepCopy(tv)));
            } else {
                diff(at.child(key), source.get(key), tv, ops);
            }
        }
    }

    private static void diffArrays(JsonPointer at, JsonArray source, JsonArray target, List<JsonPatchOperation> ops) {
        int common = Math.min(source.size(), target.size());
        for (int i = 0; i < common; i++) {
            diff(at.child(i), source.get(i), target.get(i), ops);
        }
        if (target.size() > source.size()) {
            for (int i = source.size(); i < target.size(); i++) {
                ops.add(JsonPatchOperation.add(at.child("-"), JsonCopy.deepCopy(target.get(i))));
            }
        } else if (source.size() > target.size()) {
            // remove from the tail backwards so earlier indices stay valid as we go
            for (int i = source.size() - 1; i >= target.size(); i--) {
                ops.add(JsonPatchOperation.remove(at.child(i)));
            }
        }
    }
}
