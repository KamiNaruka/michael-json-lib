package heehee.michael.json.patch;

/** Thrown when a JSON Patch document is malformed, or when applying it fails (e.g. a failed "test"). */
public class JsonPatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonPatchException(String message) {
        super(message);
    }

    public JsonPatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
