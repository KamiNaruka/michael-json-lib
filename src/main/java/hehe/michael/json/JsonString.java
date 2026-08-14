package hehe.michael.json;

import java.util.Objects;

public final class JsonString extends JsonValue {

    private final String value;

    public JsonString(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null (use JsonNull.INSTANCE instead)");
    }

    @Override
    public JsonType type() { return JsonType.STRING; }

    @Override
    public String asString() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonString)) return false;
        return value.equals(((JsonString) o).value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeString(value);
    }
}
