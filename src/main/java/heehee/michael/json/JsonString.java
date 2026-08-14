package heehee.michael.json;

import java.util.Objects;

/**
 * Immutable JSON string value.
 */
public final class JsonString extends JsonValue {

    private final String value;

    /**
     * Creates a JSON string.
     *
     * @param value string value; must not be {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public JsonString(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null (use JsonNull.INSTANCE instead)");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.STRING; }

    /**
     * {@inheritDoc}
     */
    @Override
    public String asString() { return value; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonString)) return false;
        return value.equals(((JsonString) o).value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeString(value);
    }
}
