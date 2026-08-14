package heehee.michael.json;

/**
 * Thrown when malformed JSON input is encountered. The recorded line and column are one-based; the character offset is zero-based.
 */
public class JsonParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** One-based line number where parsing failed. */
    private final int line;
    /** One-based column number where parsing failed. */
    private final int column;
    /** Zero-based character offset where parsing failed. */
    private final int offset;

    /**
     * Creates a parse exception with source-location information.
     *
     * @param message error description
     * @param line one-based line number
     * @param column one-based column number
     * @param offset zero-based character offset
     */
    public JsonParseException(String message, int line, int column, int offset) {
        super(message + " (line " + line + ", column " + column + ", offset " + offset + ")");
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    /**
     * Returns the one-based line where parsing failed.
     *
     * @return line number
     */
    public int getLine() { return line; }
    /**
     * Returns the one-based column where parsing failed.
     *
     * @return column number
     */
    public int getColumn() { return column; }
    /**
     * Returns the zero-based character offset where parsing failed.
     *
     * @return character offset
     */
    public int getOffset() { return offset; }
}
