package hehe.michael.json;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject extends JsonValue implements Iterable<Map.Entry<String, JsonValue>> {

    private final Map<String, JsonValue> members = new LinkedHashMap<>();

    public JsonObject() { }

    public JsonObject(Map<String, ? extends JsonValue> source) {
        members.putAll(source);
    }

    @Override
    public JsonType type() { return JsonType.OBJECT; }

    @Override
    public JsonObject asObject() { return this; }

    @Override
    public int size() { return members.size(); }

    public boolean containsKey(String key) { return members.containsKey(key); }

    public Set<String> keySet() { return members.keySet(); }

    public Set<Map.Entry<String, JsonValue>> entrySet() { return members.entrySet(); }

    public Collection<JsonValue> values() { return members.values(); }

    @Override
    public JsonValue get(String key) {
        JsonValue v = members.get(key);
        return v == null ? JsonNull.INSTANCE : v;
    }

    public JsonValue getOrNull(String key) {
        return members.get(key);
    }

    public JsonObject put(String key, JsonValue value) {
        members.put(key, value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonObject put(String key, String value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    public JsonObject put(String key, int value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    public JsonObject put(String key, long value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    public JsonObject put(String key, double value) {
        return put(key, new JsonNumber(BigDecimal.valueOf(value)));
    }

    public JsonObject put(String key, BigDecimal value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonNumber(value));
    }

    public JsonObject put(String key, boolean value) {
        return put(key, JsonBoolean.of(value));
    }

    public JsonObject putNull(String key) {
        return put(key, JsonNull.INSTANCE);
    }

    public JsonValue remove(String key) {
        JsonValue v = members.remove(key);
        return v == null ? JsonNull.INSTANCE : v;
    }

    public String getString(String key) { return get(key).asString(); }
    public String getString(String key, String defaultValue) {
        return containsKey(key) ? get(key).asString() : defaultValue;
    }

    public int getInt(String key) { return get(key).asInt(); }
    public int getInt(String key, int defaultValue) {
        return containsKey(key) ? get(key).asInt() : defaultValue;
    }

    public long getLong(String key) { return get(key).asLong(); }
    public long getLong(String key, long defaultValue) {
        return containsKey(key) ? get(key).asLong() : defaultValue;
    }

    public double getDouble(String key) { return get(key).asDouble(); }
    public double getDouble(String key, double defaultValue) {
        return containsKey(key) ? get(key).asDouble() : defaultValue;
    }

    public boolean getBoolean(String key) { return get(key).asBoolean(); }
    public boolean getBoolean(String key, boolean defaultValue) {
        return containsKey(key) ? get(key).asBoolean() : defaultValue;
    }

    public JsonObject getObject(String key) { return get(key).asObject(); }
    public JsonArray getArray(String key) { return get(key).asArray(); }

    @Override
    public Iterator<Map.Entry<String, JsonValue>> iterator() {
        return members.entrySet().iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonObject)) return false;
        return members.equals(((JsonObject) o).members);
    }

    @Override
    public int hashCode() { return members.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeObject(this);
    }
}
