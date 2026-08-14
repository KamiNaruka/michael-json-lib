package heehee.michael.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Mutable, insertion-ordered JSON array. Java {@code null} values added through this API are normalized to {@link JsonNull#INSTANCE}.
 */
public final class JsonArray extends JsonValue implements Iterable<JsonValue> {

    private final List<JsonValue> items = new ArrayList<>();

    /**
     * Creates an empty JSON array.
     */
    public JsonArray() { }

    /**
     * Creates an array containing the supplied values in iteration order. Java {@code null} elements are stored as JSON null.
     *
     * @param source source values; must not be {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public JsonArray(List<? extends JsonValue> source) {
        Objects.requireNonNull(source, "source");
        for (JsonValue value : source) add(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.ARRAY; }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonArray asArray() { return this; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() { return items.size(); }

    /**
     * Returns the element at {@code index}.
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code [0, size())}
     */
    @Override
    public JsonValue get(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException(
                    "index " + index + " out of range (size=" + items.size() + ")");
        }
        return items.get(index);
    }

    /**
     * Appends a JSON value. Java {@code null} is stored as JSON null.
     *
     * @param value value to append
     * @return this array
     */
    public JsonArray add(JsonValue value) {
        items.add(value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    /**
     * Appends a string, or JSON null when the Java value is {@code null}.
     *
     * @param value string value
     * @return this array
     */
    public JsonArray add(String value) {
        return add(value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    /**
     * Appends a int value.
     *
     * @param value value to append
     * @return this array
     */
    public JsonArray add(int value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Appends a long value.
     *
     * @param value value to append
     * @return this array
     */
    public JsonArray add(long value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Appends a double value.
     *
     * @param value value to append
     * @return this array
     * @throws IllegalArgumentException if {@code value} is NaN or infinite
     */
    public JsonArray add(double value) { return add(new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Appends a boolean value.
     *
     * @param value value to append
     * @return this array
     */
    public JsonArray add(boolean value) { return add(JsonBoolean.of(value)); }
    /**
     * Appends JSON null.
     *
     * @return this array
     */
    public JsonArray addNull() { return add(JsonNull.INSTANCE); }

    /**
     * Inserts {@code value} at {@code index}, shifting the element currently at
     * that index (and all subsequent elements) one position to the right.
     * <p>
     * {@code index} may be anywhere from {@code 0} to {@link #size()} inclusive;
     * an index equal to {@code size()} appends, matching the semantics needed by
     * JSON Patch ("RFC 6902") {@code add} operations targeting {@code /-} or an
     * explicit end-of-array index. A Java {@code null} value is stored as {@link JsonNull}.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value value to insert; Java {@code null} becomes JSON {@code null}
     * @return this array
     * @throws IndexOutOfBoundsException if index is negative or greater than {@link #size()}
     */
    public JsonArray add(int index, JsonValue value) {
        checkInsertIndex(index);
        items.add(index, value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    /**
     * Inserts a string at the requested position, or JSON null when the Java value is {@code null}.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value string value
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     */
    public JsonArray add(int index, String value) {
        return add(index, value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    /**
     * Inserts a int value at the requested position.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value value to insert
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     */
    public JsonArray add(int index, int value) { return add(index, new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Inserts a long value at the requested position.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value value to insert
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     */
    public JsonArray add(int index, long value) { return add(index, new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Inserts a double value at the requested position.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value value to insert
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     * @throws IllegalArgumentException if {@code value} is NaN or infinite
     */
    public JsonArray add(int index, double value) { return add(index, new JsonNumber(BigDecimal.valueOf(value))); }
    /**
     * Inserts a boolean value at the requested position.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @param value value to insert
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     */
    public JsonArray add(int index, boolean value) { return add(index, JsonBoolean.of(value)); }
    /**
     * Inserts JSON null at the requested position.
     *
     * @param index insertion index from {@code 0} through {@link #size()} inclusive
     * @return this array
     * @throws IndexOutOfBoundsException if the index is outside the insertion range
     */
    public JsonArray addNull(int index) { return add(index, JsonNull.INSTANCE); }

    private void checkInsertIndex(int index) {
        if (index < 0 || index > items.size()) {
            throw new IndexOutOfBoundsException(
                    "insert index " + index + " out of range (size=" + items.size() + ")");
        }
    }

    /**
     * Removes and returns the element at an index.
     *
     * @param index element index
     * @return removed value
     * @throws IndexOutOfBoundsException if the index is outside the array
     */
    public JsonValue remove(int index) { return items.remove(index); }

    /**
     * Returns the element at an index as a string.
     *
     * @param index element index
     * @return string value
     */
    public String getString(int index) { return get(index).asString(); }
    /**
     * Returns the element at an index as a int.
     *
     * @param index element index
     * @return int value
     */
    public int getInt(int index) { return get(index).asInt(); }
    /**
     * Returns the element at an index as a long.
     *
     * @param index element index
     * @return long value
     */
    public long getLong(int index) { return get(index).asLong(); }
    /**
     * Returns the element at an index as a double.
     *
     * @param index element index
     * @return double value
     */
    public double getDouble(int index) { return get(index).asDouble(); }
    /**
     * Returns the element at an index as a boolean.
     *
     * @param index element index
     * @return boolean value
     */
    public boolean getBoolean(int index) { return get(index).asBoolean(); }
    /**
     * Returns the element at an index as a JSON object.
     *
     * @param index element index
     * @return JSON object value
     */
    public JsonObject getObject(int index) { return get(index).asObject(); }
    /**
     * Returns the element at an index as a JSON array.
     *
     * @param index element index
     * @return JSON array value
     */
    public JsonArray getArray(int index) { return get(index).asArray(); }

    /**
     * Returns an unmodifiable view of this array. Changes made through this {@code JsonArray} remain visible in the view.
     *
     * @return unmodifiable list view
     */
    public List<JsonValue> asList() {
        return Collections.unmodifiableList(items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<JsonValue> iterator() { return items.iterator(); }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonArray)) return false;
        return items.equals(((JsonArray) o).items);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return items.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeArray(this);
    }
}
