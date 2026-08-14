package heehee.michael.json;

/**
 * The six JSON value kinds represented by {@link JsonValue}.
 */
public enum JsonType {
    /** JSON object value. */
    OBJECT,
    /** JSON array value. */
    ARRAY,
    /** JSON string value. */
    STRING,
    /** JSON number value. */
    NUMBER,
    /** JSON boolean value. */
    BOOLEAN,
    /** JSON null value. */
    NULL
}
