package hehe.michael.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Json {

    private Json() { }

    public static JsonValue parse(String text) {
        return new JsonParser(text).parse();
    }

    public static JsonValue parse(Reader reader) {
        return parse(readAll(reader));
    }

    public static JsonValue parseFile(Path path) throws IOException {
        return parse(Files.readString(path, StandardCharsets.UTF_8));
    }

    public static String stringify(JsonValue value) {
        StringBuilder sb = new StringBuilder();
        try {
            new JsonWriter(sb, 0).write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    public static String stringifyPretty(JsonValue value) {
        return stringifyPretty(value, 2);
    }

    public static String stringifyPretty(JsonValue value, int indentSize) {
        StringBuilder sb = new StringBuilder();
        try {
            new JsonWriter(sb, indentSize).write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    public static void write(JsonValue value, Writer writer, boolean pretty) throws IOException {
        new JsonWriter(writer, pretty ? 2 : 0).write(value);
    }

    public static void writeToFile(JsonValue value, Path path, boolean pretty) throws IOException {
        Files.writeString(path, pretty ? stringifyPretty(value) : stringify(value), StandardCharsets.UTF_8);
    }

    public static JsonObject object() { return new JsonObject(); }
    public static JsonArray array() { return new JsonArray(); }

    private static String readAll(Reader reader) {
        try (BufferedReader br = new BufferedReader(reader)) {
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
