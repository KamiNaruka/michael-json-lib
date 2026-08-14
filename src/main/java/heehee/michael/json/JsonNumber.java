package heehee.michael.json;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable JSON number backed by {@link java.math.BigDecimal}. Equality is numeric, so values such as {@code 1} and {@code 1.0} compare equal.
 */
public final class JsonNumber extends JsonValue {

    private final BigDecimal value;

    /**
     * Creates a JSON number from a non-null {@link java.math.BigDecimal}.
     *
     * @param value numeric value
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public JsonNumber(BigDecimal value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Creates a JSON number from a finite {@code double}.
     *
     * @param value numeric value
     * @throws IllegalArgumentException if {@code value} is NaN or infinite
     */
    public JsonNumber(double value) { this(finiteBigDecimal(value)); }
    /**
     * Creates a JSON number from a {@code long}.
     *
     * @param value numeric value
     */
    public JsonNumber(long value) { this(BigDecimal.valueOf(value)); }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonType type() { return JsonType.NUMBER; }

    /**
     * {@inheritDoc}
     */
    @Override
    public int asInt() { return value.intValueExact(); }

    /**
     * {@inheritDoc}
     */
    @Override
    public long asLong() { return value.longValueExact(); }

    /**
     * {@inheritDoc}
     */
    @Override
    public double asDouble() {
        double d = value.doubleValue();
        if (!Double.isFinite(d)) throw new ArithmeticException("JSON number is outside the finite double range: " + value);
        return d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal asBigDecimal() { return value; }

    /**
     * Returns whether this number has no fractional component after trailing zeros are ignored.
     *
     * @return whether the value is mathematically integral
     */
    public boolean isIntegral() {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private static BigDecimal finiteBigDecimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON numbers cannot be NaN or Infinity: " + value);
        }
        return BigDecimal.valueOf(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonNumber)) return false;
        return value.compareTo(((JsonNumber) o).value) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() { return value.stripTrailingZeros().hashCode(); }

    @Override
    void write(JsonWriter writer) throws java.io.IOException {
        writer.writeNumber(value);
    }
}
