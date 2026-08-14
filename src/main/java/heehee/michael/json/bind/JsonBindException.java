package heehee.michael.json.bind;

/** Thrown by {@link JsonMapper} when a value can't be converted to or from JSON. */
public class JsonBindException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonBindException(String message) {
        super(message);
    }

    public JsonBindException(String message, Throwable cause) {
        super(message, cause);
    }
}
