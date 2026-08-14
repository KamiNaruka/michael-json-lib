package heehee.michael.json.patch;

/** Thrown when a JSON Patch document is malformed, or when applying it fails (e.g. a failed "test"). */
public class JsonPatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a JSON Patch error.
     *
     * @param message error description
     */
    public JsonPatchException(String message) {
        super(message);
    }

    /**
     * Creates a JSON Patch error with its underlying cause.
     *
     * @param message error description
     * @param cause underlying failure
     */
    public JsonPatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
