package hehe.michael.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class JsonArray extends JsonValue implements Iterable<JsonValue> {

    private final List<JsonValue> items = new ArrayList<>();

    public JsonArray() { }

    public JsonArray(List<? extends JsonValue> source) {
        items.addAll(source);
    }

    @Override
    public JsonType type() { return JsonType.ARRAY; }

    @Override
    public JsonArray asArray() { return this; }

    @Override
    public int size() { return items.size(); }

    @Override
    public JsonValue get(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " out of range (size=" + items.size() + ")");
        }
        return items.get(index);
    }

    public JsonArray add(JsonValue value) {
        items.add(value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonArray add(String value) {
        return add(value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    public JsonArray add(int value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    public JsonArray add(long value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    public JsonArray add(double value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    public JsonArray add(boolean value) { return add(JsonBoolean.of(value)); }
    public JsonArray addNull() { return add(JsonNull.INSTANCE); }

    public JsonValue remove(int index) { return items.remove(index); }

    public String getString(int index) { return get(index).asString(); }
    public int getInt(int index) { return get(index).asInt(); }
    public long getLong(int index) { return get(index).asLong(); }
    public double getDouble(int index) { return get(index).asDouble(); }
    public boolean getBoolean(int index) { return get(index).asBoolean(); }
    public JsonObject getObject(int index) { return get(index).asObject(); }
    public JsonArray getArray(int index) { return get(index).asArray(); }

    public List<JsonValue> asList() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public Iterator<JsonValue> iterator() { return items.iterator(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonArray)) return false;
        return items.equals(((JsonArray) o).items);
    }

    @Override
    public int hashCode() { return items.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeArray(this);
    }
}
