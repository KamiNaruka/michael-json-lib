package hehe.michael.json;

public class JsonParseException extends RuntimeException {

    private final int line;
    private final int column;
    private final int offset;

    public JsonParseException(String message, int line, int column, int offset) {
        super(message + " (line " + line + ", column " + column + ", offset " + offset + ")");
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public int getOffset() { return offset; }
}
