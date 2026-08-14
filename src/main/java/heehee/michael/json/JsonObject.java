package heehee.michael.json;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mutable JSON object that preserves insertion order. Java {@code null} values stored through this API are normalized to {@link JsonNull#INSTANCE}.
 */
public final class JsonObject extends JsonValue implements Iterable<Map.Entry<String, JsonValue>> {

    private final Map<String, JsonValue> members = new LinkedHashMap<>();

    /**
     * Creates an empty JSON object.
     */
    public JsonObject() { }

    /**
     * Creates an object from the supplied entries, preserving their iteration order. Java {@code null} values become JSON null.
     *
     * @param source source members; the map and its keys must not be {@code null}
     * @throws NullPointerException if {@code source} or one of its keys is {@code null}
     */
    public JsonObject(Map<String, ? extends JsonValue> source) {
        Objects.requireNonNull(source, "source");
        for (Map.Entry<String, ? extends JsonValue> e : source.entrySet()) {
            put(Objects.requireNonNull(e.getKey(), "JSON object key must not be null"), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.OBJECT; }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject asObject() { return this; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() { return members.size(); }

    /**
     * Returns whether a member with the supplied name is present.
     *
     * @param key member name
     * @return whether the member exists
     */
    public boolean containsKey(String key) { return members.containsKey(key); }

    /**
     * Returns an unmodifiable view of the member names in insertion order.
     *
     * @return member-name view
     */
    public Set<String> keySet() { return Collections.unmodifiableMap(members).keySet(); }

    /**
     * Returns an unmodifiable view of the entries in insertion order.
     *
     * @return entry view
     */
    public Set<Map.Entry<String, JsonValue>> entrySet() { return Collections.unmodifiableMap(members).entrySet(); }

    /**
     * Returns an unmodifiable view of member values in insertion order.
     *
     * @return value view
     */
    public Collection<JsonValue> values() { return Collections.unmodifiableMap(members).values(); }

    /**
     * Returns the value stored under {@code key}; if the member is absent, returns {@link JsonNull#INSTANCE} rather than Java {@code null}.
     */
    @Override
    public JsonValue get(String key) {
        JsonValue v = members.get(key);
        return v == null ? JsonNull.INSTANCE : v;
    }

    /**
     * Returns the stored member value, or Java {@code null} when the member is absent.
     *
     * @param key member name
     * @return stored value or Java {@code null}
     */
    public JsonValue getOrNull(String key) {
        return members.get(key);
    }

    /**
     * Adds or replaces a member. Java {@code null} is stored as JSON null.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public JsonObject put(String key, JsonValue value) {
        members.put(Objects.requireNonNull(key, "JSON object key must not be null"), value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    /**
     * Adds or replaces a string member; Java {@code null} is stored as JSON null.
     *
     * @param key non-null member name
     * @param value string value
     * @return this object
     */
    public JsonObject put(String key, String value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    /**
     * Adds or replaces a numeric member.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     */
    public JsonObject put(String key, int value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    /**
     * Adds or replaces a numeric member.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     */
    public JsonObject put(String key, long value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    /**
     * Adds or replaces a numeric member.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     * @throws IllegalArgumentException if {@code value} is NaN or infinite
     */
    public JsonObject put(String key, double value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    /**
     * Adds or replaces a numeric member. Java {@code null} is stored as JSON null.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     */
    public JsonObject put(String key, BigDecimal value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonNumber(value));
    }

    /**
     * Adds or replaces a boolean member.
     *
     * @param key non-null member name
     * @param value member value
     * @return this object
     */
    public JsonObject put(String key, boolean value) {
        return put(key, JsonBoolean.of(value));
    }

    /**
     * Adds or replaces a member with JSON null.
     *
     * @param key non-null member name
     * @return this object
     */
    public JsonObject putNull(String key) {
        return put(key, JsonNull.INSTANCE);
    }

    /**
     * Removes a member.
     *
     * @param key member name
     * @return removed value, or {@link JsonNull#INSTANCE} if the member was absent
     */
    public JsonValue remove(String key) {
        JsonValue v = members.remove(key);
        return v == null ? JsonNull.INSTANCE : v;
    }

    /**
     * Returns a member as a string. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return string value
     */
    public String getString(String key) { return get(key).asString(); }
    /**
     * Returns a member as a string, using the supplied default only when the key is absent. A present JSON null value is not treated as missing.
     *
     * @param key member name
     * @param defaultValue value to use when the key is absent
     * @return string member value or the default
     */
    public String getString(String key, String defaultValue) {
        return containsKey(key) ? get(key).asString() : defaultValue;
    }

    /**
     * Returns a member as a int. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return int value
     */
    public int getInt(String key) { return get(key).asInt(); }
    /**
     * Returns a member as a int, using the supplied default only when the key is absent. A present JSON null value is not treated as missing.
     *
     * @param key member name
     * @param defaultValue value to use when the key is absent
     * @return int member value or the default
     */
    public int getInt(String key, int defaultValue) {
        return containsKey(key) ? get(key).asInt() : defaultValue;
    }

    /**
     * Returns a member as a long. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return long value
     */
    public long getLong(String key) { return get(key).asLong(); }
    /**
     * Returns a member as a long, using the supplied default only when the key is absent. A present JSON null value is not treated as missing.
     *
     * @param key member name
     * @param defaultValue value to use when the key is absent
     * @return long member value or the default
     */
    public long getLong(String key, long defaultValue) {
        return containsKey(key) ? get(key).asLong() : defaultValue;
    }

    /**
     * Returns a member as a double. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return double value
     */
    public double getDouble(String key) { return get(key).asDouble(); }
    /**
     * Returns a member as a double, using the supplied default only when the key is absent. A present JSON null value is not treated as missing.
     *
     * @param key member name
     * @param defaultValue value to use when the key is absent
     * @return double member value or the default
     */
    public double getDouble(String key, double defaultValue) {
        return containsKey(key) ? get(key).asDouble() : defaultValue;
    }

    /**
     * Returns a member as a boolean. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return boolean value
     */
    public boolean getBoolean(String key) { return get(key).asBoolean(); }
    /**
     * Returns a member as a boolean, using the supplied default only when the key is absent. A present JSON null value is not treated as missing.
     *
     * @param key member name
     * @param defaultValue value to use when the key is absent
     * @return boolean member value or the default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return containsKey(key) ? get(key).asBoolean() : defaultValue;
    }

    /**
     * Returns a member as a JSON object. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return JSON object value
     */
    public JsonObject getObject(String key) { return get(key).asObject(); }
    /**
     * Returns a member as a JSON array. Missing members are represented as JSON null and therefore fail the type conversion.
     *
     * @param key member name
     * @return JSON array value
     */
    public JsonArray getArray(String key) { return get(key).asArray(); }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Map.Entry<String, JsonValue>> iterator() {
        return entrySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonObject)) return false;
        return members.equals(((JsonObject) o).members);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return members.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeObject(this);
    }
}
