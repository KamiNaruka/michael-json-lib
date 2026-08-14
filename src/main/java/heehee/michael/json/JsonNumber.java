package heehee.michael.json;

import java.math.BigDecimal;
import java.util.Objects;

public final class JsonNumber extends JsonValue {

    private final BigDecimal value;

    public JsonNumber(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public JsonNumber(double value) { this(finiteBigDecimal(value)); }
    public JsonNumber(long value) { this(BigDecimal.valueOf(value)); }

    @Override
    public JsonType type() { return JsonType.NUMBER; }

    @Override
    public int asInt() { return value.intValueExact(); }

    @Override
    public long asLong() { return value.longValueExact(); }

    @Override
    public double asDouble() {
        double d = value.doubleValue();
        if (!Double.isFinite(d)) throw new ArithmeticException("JSON number is outside the finite double range: " + value);
        return d;
    }

    @Override
    public BigDecimal asBigDecimal() { return value; }

    public boolean isIntegral() {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private static BigDecimal finiteBigDecimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON numbers cannot be NaN or Infinity: " + value);
        }
        return BigDecimal.valueOf(value);
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
