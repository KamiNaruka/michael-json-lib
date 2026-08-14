package hehe.michael.json;

import java.math.BigDecimal;
import java.util.Objects;

public final class JsonNumber extends JsonValue {

    private final BigDecimal value;

    public JsonNumber(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public JsonNumber(double value) { this(BigDecimal.valueOf(value)); }
    public JsonNumber(long value) { this(BigDecimal.valueOf(value)); }

    @Override
    public JsonType type() { return JsonType.NUMBER; }

    @Override
    public int asInt() { return value.intValue(); }

    @Override
    public long asLong() { return value.longValue(); }

    @Override
    public double asDouble() { return value.doubleValue(); }

    @Override
    public BigDecimal asBigDecimal() { return value; }

    public boolean isIntegral() {
        return value.stripTrailingZeros().scale() <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonNumber)) return false;
        return value.compareTo(((JsonNumber) o).value) == 0;
    }

    @Override
    public int hashCode() { return value.stripTrailingZeros().hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeNumber(value);
    }
}
