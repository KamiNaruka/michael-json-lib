package heehee.michael.json.patch;

/** Thrown for malformed JSON Pointer strings, or when a pointer fails to resolve against a document. */
public class JsonPointerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a JSON Pointer error.
     *
     * @param message error description
     */
    public JsonPointerException(String message) {
        super(message);
    }

    /**
     * Creates a JSON Pointer error with its underlying cause.
     *
     * @param message error description
     * @param cause underlying failure
     */
    public JsonPointerException(String message, Throwable cause) {
        super(message, cause);
    }
}
