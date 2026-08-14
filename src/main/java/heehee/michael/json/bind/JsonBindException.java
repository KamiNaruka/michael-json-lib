package heehee.michael.json.bind;

/** Thrown by {@link JsonMapper} when a value can't be converted to or from JSON. */
public class JsonBindException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a binding error.
     *
     * @param message error description
     */
    public JsonBindException(String message) {
        super(message);
    }

    /**
     * Creates a binding error with its underlying cause.
     *
     * @param message error description
     * @param cause underlying failure
     */
    public JsonBindException(String message, Throwable cause) {
        super(message, cause);
    }
}
