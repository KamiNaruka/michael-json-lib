package hehe.michael.json;

public final class JsonBoolean extends JsonValue {

    public static final JsonBoolean TRUE = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    private JsonBoolean(boolean value) { this.value = value; }

    public static JsonBoolean of(boolean value) { return value ? TRUE : FALSE; }

    @Override
    public JsonType type() { return JsonType.BOOLEAN; }

    @Override
    public boolean asBoolean() { return value; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof JsonBoolean && ((JsonBoolean) o).value == value);
    }

    @Override
    public int hashCode() { return Boolean.hashCode(value); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeBoolean(value);
    }
}
