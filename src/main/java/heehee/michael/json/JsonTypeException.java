package heehee.michael.json;

/**
 * Thrown when a type-specific JSON operation is used on an incompatible value type.
 */
public class JsonTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    /**
     * Creates a JSON type error.
     *
     * @param message error description
     */
    public JsonTypeException(String message) {
        super(message);
    }
}
