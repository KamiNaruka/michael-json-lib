package heehee.michael.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Convenience API for parsing, serializing, creating, and writing the library's JSON tree model.
 */
public final class Json {

    private Json() { }

    /**
     * Parses exactly one complete JSON value from a string.
     *
     * @param text JSON text
     * @return parsed JSON value
     * @throws JsonParseException if the text is not valid JSON or contains trailing non-whitespace data
     */
    public static JsonValue parse(String text) {
        return new JsonParser(text).parse();
    }

    /**
     * Reads all characters from a reader and parses exactly one complete JSON value. The reader is not closed.
     *
     * @param reader character source
     * @return parsed JSON value
     * @throws java.io.UncheckedIOException if reading fails
     * @throws JsonParseException if the input is not valid JSON
     */
    public static JsonValue parse(Reader reader) {
        return parse(readAll(reader));
    }

    /**
     * Reads a UTF-8 file and parses exactly one complete JSON value.
     *
     * @param path file to read
     * @return parsed JSON value
     * @throws java.io.IOException if the file cannot be read
     * @throws JsonParseException if the file does not contain valid JSON
     */
    public static JsonValue parseFile(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Serializes a JSON tree to compact JSON text.
     *
     * @param value JSON value to serialize
     * @return compact JSON text
     */
    public static String stringify(JsonValue value) {
        StringBuilder sb = new StringBuilder();
        try {
            new JsonWriter(sb, 0).write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    /**
     * Serializes a JSON tree using the default two-space indentation.
     *
     * @param value JSON value to serialize
     * @return pretty-printed JSON text
     */
    public static String stringifyPretty(JsonValue value) {
        return stringifyPretty(value, 2);
    }

    /**
     * Serializes a JSON tree using the requested number of spaces per nesting level. A non-positive indentation size produces compact output.
     *
     * @param value JSON value to serialize
     * @param indentSize spaces per indentation level
     * @return serialized JSON text
     */
    public static String stringifyPretty(JsonValue value, int indentSize) {
        StringBuilder sb = new StringBuilder();
        try {
            new JsonWriter(sb, indentSize).write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    /**
     * Writes a JSON tree to a writer. Pretty mode uses two-space indentation. The writer is not closed.
     *
     * @param value JSON value to serialize
     * @param writer destination writer
     * @param pretty whether to pretty-print
     * @throws java.io.IOException if writing fails
     */
    public static void write(JsonValue value, Writer writer, boolean pretty) throws IOException {
        new JsonWriter(writer, pretty ? 2 : 0).write(value);
    }

    /**
     * Serializes a JSON tree and writes it to a UTF-8 file.
     *
     * @param value JSON value to serialize
     * @param path destination file
     * @param pretty whether to pretty-print with two-space indentation
     * @throws java.io.IOException if the file cannot be written
     */
    public static void writeToFile(JsonValue value, Path path, boolean pretty) throws IOException {
        Files.writeString(path, pretty ? stringifyPretty(value) : stringify(value), StandardCharsets.UTF_8);
    }

    /**
     * Creates an empty mutable JSON object.
     *
     * @return new empty object
     */
    public static JsonObject object() { return new JsonObject(); }
    /**
     * Creates an empty mutable JSON array.
     *
     * @return new empty array
     */
    public static JsonArray array() { return new JsonArray(); }

    private static String readAll(Reader reader) {
        BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
