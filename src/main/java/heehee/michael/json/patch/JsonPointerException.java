package heehee.michael.json.patch;

/** Thrown for malformed JSON Pointer strings, or when a pointer fails to resolve against a document. */
public class JsonPointerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonPointerException(String message) {
        super(message);
    }

    public JsonPointerException(String message, Throwable cause) {
        super(message, cause);
    }
}
