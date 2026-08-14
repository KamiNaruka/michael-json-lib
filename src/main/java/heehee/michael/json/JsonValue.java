package heehee.michael.json;

import java.math.BigDecimal;

/**
 * Base type for all JSON tree values. Type-specific conversion methods throw {@link JsonTypeException} unless overridden by the matching subtype.
 */
public abstract class JsonValue {

    /**
     * Returns the JSON value kind.
     *
     * @return this value's type
     */
    public abstract JsonType type();

    /**
     * Returns whether this value is a JSON object.
     *
     * @return {@code true} when this value is a JSON object
     */
    public boolean isObject()  { return type() == JsonType.OBJECT; }
    /**
     * Returns whether this value is a JSON array.
     *
     * @return {@code true} when this value is a JSON array
     */
    public boolean isArray()   { return type() == JsonType.ARRAY; }
    /**
     * Returns whether this value is a JSON string.
     *
     * @return {@code true} when this value is a JSON string
     */
    public boolean isString()  { return type() == JsonType.STRING; }
    /**
     * Returns whether this value is a JSON number.
     *
     * @return {@code true} when this value is a JSON number
     */
    public boolean isNumber()  { return type() == JsonType.NUMBER; }
    /**
     * Returns whether this value is a JSON boolean.
     *
     * @return {@code true} when this value is a JSON boolean
     */
    public boolean isBoolean() { return type() == JsonType.BOOLEAN; }
    /**
     * Returns whether this value is a JSON null.
     *
     * @return {@code true} when this value is a JSON null
     */
    public boolean isNull()    { return type() == JsonType.NULL; }

    /**
     * Returns this value as a JSON object.
     *
     * @return converted JsonObject value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public JsonObject asObject() {
        throw new JsonTypeException("Not a JSON object: " + type());
    }

    /**
     * Returns this value as a JSON array.
     *
     * @return converted JsonArray value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public JsonArray asArray() {
        throw new JsonTypeException("Not a JSON array: " + type());
    }

    /**
     * Returns this value as a JSON string.
     *
     * @return converted String value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public String asString() {
        throw new JsonTypeException("Not a JSON string: " + type());
    }

    /**
     * Returns this value as a JSON boolean.
     *
     * @return converted boolean value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public boolean asBoolean() {
        throw new JsonTypeException("Not a JSON boolean: " + type());
    }

    /**
     * Returns this value as a JSON number.
     *
     * @return converted int value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public int asInt() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    /**
     * Returns this value as a JSON number.
     *
     * @return converted long value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public long asLong() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    /**
     * Returns this value as a JSON number.
     *
     * @return converted double value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public double asDouble() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    /**
     * Returns this value as a JSON number.
     *
     * @return converted BigDecimal value
     * @throws JsonTypeException if this value has a different JSON type
     */
    public BigDecimal asBigDecimal() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    /**
     * Object-member accessor for subclasses that represent JSON objects.
     *
     * @param key member name
     * @return addressed member
     * @throws JsonTypeException if this value is not a JSON object
     */
    public JsonValue get(String key) {
        throw new JsonTypeException("get(String) only works on a JSON object, this is: " + type());
    }

    /**
     * Array-element accessor for subclasses that represent JSON arrays.
     *
     * @param index element index
     * @return addressed element
     * @throws JsonTypeException if this value is not a JSON array
     */
    public JsonValue get(int index) {
        throw new JsonTypeException("get(int) only works on a JSON array, this is: " + type());
    }

    /**
     * Traverses objects and arrays using a dot-separated path. Object segments are member names and array segments are decimal indexes. Empty path segments are ignored; any unresolved segment returns {@link JsonNull#INSTANCE}.
     *
     * @param dotPath dot-separated path
     * @return resolved value, or JSON null if traversal cannot continue
     */
    public JsonValue path(String dotPath) {
        JsonValue current = this;
        for (String part : dotPath.split("\\.")) {
            if (part.isEmpty()) continue;
            if (current.isObject()) {
                current = current.asObject().get(part);
            } else if (current.isArray()) {
                JsonArray arr = current.asArray();
                try {
                    int idx = Integer.parseInt(part);
                    if (idx < 0 || idx >= arr.size()) return JsonNull.INSTANCE;
                    current = arr.get(idx);
                } catch (NumberFormatException e) {
                    return JsonNull.INSTANCE;
                }
            } else {
                return JsonNull.INSTANCE;
            }
        }
        return current;
    }

    /**
     * Returns the number of members or elements for container values.
     *
     * @return container size
     * @throws JsonTypeException if this value is not an object or array
     */
    public int size() {
        throw new JsonTypeException("size() only works on object/array, this is: " + type());
    }

    /**
     * Returns whether this container has no members or elements.
     *
     * @return {@code true} when {@link #size()} is zero
     * @throws JsonTypeException if this value is not an object or array
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Json.stringify(this);
    }

    /**
     * Serializes this value using the default pretty-printing format.
     *
     * @return pretty-printed JSON text
     */
    public String toPrettyString() {
        return Json.stringifyPretty(this);
    }

    abstract void write(JsonWriter writer) throws java.io.IOException;
}
