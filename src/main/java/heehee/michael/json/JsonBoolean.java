package heehee.michael.json;

/**
 * Immutable JSON boolean value. Use {@link #TRUE}, {@link #FALSE}, or {@link #of(boolean)}.
 */
public final class JsonBoolean extends JsonValue {

    /**
     * Shared JSON {@code true} value.
     */
    public static final JsonBoolean TRUE = new JsonBoolean(true);
    /**
     * Shared JSON {@code false} value.
     */
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    private JsonBoolean(boolean value) { this.value = value; }

    /**
     * Returns the shared JSON boolean instance for a Java boolean.
     *
     * @param value boolean value
     * @return {@link #TRUE} or {@link #FALSE}
     */
    public static JsonBoolean of(boolean value) { return value ? TRUE : FALSE; }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.BOOLEAN; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean asBoolean() { return value; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof JsonBoolean && ((JsonBoolean) o).value == value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return Boolean.hashCode(value); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeBoolean(value);
    }
}
