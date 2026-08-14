package heehee.michael.json;

public final class JsonNull extends JsonValue {

    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() { }

    @Override
    public JsonType type() { return JsonType.NULL; }

    @Override
    public boolean equals(Object o) { return o instanceof JsonNull; }

    @Override
    public int hashCode() { return 0; }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeNull();
    }
}
