package heehee.michael.json;

/**
 * Singleton representation of the JSON {@code null} literal.
 */
public final class JsonNull extends JsonValue {

    /**
     * The single JSON null instance.
     */
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() { }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.NULL; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) { return o instanceof JsonNull; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return 0; }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeNull();
    }
}
