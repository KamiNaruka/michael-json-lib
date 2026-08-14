package heehee.michael.json.stream;

import heehee.michael.json.JsonArray;
import heehee.michael.json.JsonBoolean;
import heehee.michael.json.JsonNull;
import heehee.michael.json.JsonNumber;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonParseException;
import heehee.michael.json.JsonString;
import heehee.michael.json.JsonValue;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Reads JSON incrementally from a {@link Reader} without ever buffering the whole
 * source in memory, unlike {@link heehee.michael.json.Json#parse(Reader)}.
 *
 * <p>Two modes are supported:
 * <ul>
 *   <li><b>Sequential top-level values</b> via {@link #hasNext()}/{@link #next()} (or
 *       {@link #iterator()}/{@link #stream()}) &mdash; reads a sequence of whitespace-separated
 *       JSON values one at a time, which is exactly NDJSON (one JSON value per line).</li>
 *   <li><b>Array elements</b> via {@link #readArrayElements()} &mdash; streams the elements of a
 *       single huge top-level JSON array one at a time, without ever materializing the full array.</li>
 * </ul>
 *
 * <p>Each individual value/element read is still built as a normal in-memory {@link JsonValue}
 * tree; it's only the surrounding sequence/array that is never fully buffered.
 *
 * <p>Not thread-safe; close with {@link #close()} (or try-with-resources) when done.
 */
public final class JsonStreamReader implements Closeable {

    private final Tokenizer tokenizer;

    /**
     * Creates a streaming reader over the supplied character source. Closing this object closes the supplied reader.
     *
     * @param reader character source
     */
    public JsonStreamReader(Reader reader) {
        Reader buffered = (reader instanceof BufferedReader || reader instanceof PushbackReader)
                ? reader : new BufferedReader(reader);
        this.tokenizer = new Tokenizer(new PushbackReader(buffered, 1));
    }

    /** Opens a file for low-memory reading, decoded as UTF-8. */
    public static JsonStreamReader open(Path path) throws IOException {
        return new JsonStreamReader(Files.newBufferedReader(path, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    // Sequential top-level values / NDJSON
    // ------------------------------------------------------------------

    /** Returns {@code true} if there is at least one more top-level JSON value in the stream. */
    public boolean hasNext() throws IOException {
        tokenizer.skipWhitespace();
        return tokenizer.peek() != -1;
    }

    /** Reads and returns the next top-level JSON value from the stream. */
    public JsonValue next() throws IOException {
        if (!hasNext()) throw new NoSuchElementException("No more JSON values in the stream");
        return tokenizer.parseValue();
    }

    /**
     * Iterates the stream's top-level values. {@link IOException}s are rethrown wrapped
     * in {@link UncheckedIOException}, since {@link Iterator} can't declare checked exceptions.
     */
    public Iterator<JsonValue> iterator() {
        return new Iterator<>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                try {
                    return JsonStreamReader.this.hasNext();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public JsonValue next() {
                try {
                    return JsonStreamReader.this.next();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    /** A lazy, non-parallel {@link Stream} view over {@link #iterator()}. */
    public Stream<JsonValue> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    // ------------------------------------------------------------------
    // Streaming the elements of one top-level array
    // ------------------------------------------------------------------

    /**
     * Consumes a top-level {@code '['} and returns a lazy iterator over its elements, reading
     * (and discarding) one element at a time as the iterator advances &mdash; the array itself
     * is never held fully in memory. The iterator's {@code hasNext()}/{@code next()} wrap
     * {@link IOException}s as {@link UncheckedIOException}.
     *
     * @throws IOException if the next non-whitespace character isn't {@code '['}
     */
    public Iterator<JsonValue> readArrayElements() throws IOException {
        tokenizer.skipWhitespace();
        tokenizer.expect('[');
        return new ArrayElementIterator();
    }

    private final class ArrayElementIterator implements Iterator<JsonValue> {
        private JsonValue buffered;
        private boolean bufferedReady;
        private boolean exhausted;
        private boolean sawFirstElement;

        ArrayElementIterator() {
            try {
                tokenizer.skipWhitespace();
                if (tokenizer.peek() == ']') {
                    tokenizer.read();
                    exhausted = true;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private void computeNext() {
            if (bufferedReady || exhausted) return;
            try {
                if (sawFirstElement) {
                    tokenizer.skipWhitespace();
                    int c = tokenizer.read();
                    if (c == ']') {
                        exhausted = true;
                        return;
                    }
                    if (c != ',') {
                        throw tokenizer.error("Expected ',' or ']' in array but found '" + describe(c) + "'");
                    }
                }
                buffered = tokenizer.parseValue();
                bufferedReady = true;
                sawFirstElement = true;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            computeNext();
            return !exhausted;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonValue next() {
            computeNext();
            if (exhausted) throw new NoSuchElementException("No more elements in the array");
            bufferedReady = false;
            return buffered;
        }
    }

    private static String describe(int c) {
        return c == -1 ? "<end of input>" : String.valueOf((char) c);
    }

    /**
     * Closes the underlying reader.
     *
     * @throws java.io.IOException if closing the reader fails
     */
    @Override
    public void close() throws IOException {
        tokenizer.close();
    }

    // ------------------------------------------------------------------
    // Reader-based recursive-descent tokenizer/parser (mirrors the core JsonParser's
    // grammar, but pulls one character at a time from a Reader instead of a String).
    // ------------------------------------------------------------------

    private static final class Tokenizer implements Closeable {
        private final PushbackReader in;
        private int pos = 0;
        private int line = 1;
        private int col = 1;

        Tokenizer(PushbackReader in) {
            this.in = in;
        }

        JsonValue parseValue() throws IOException {
            skipWhitespace();
            int c = peek();
            if (c == -1) throw error("Unexpected end of input, expected a JSON value");
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return new JsonString(parseString());
                case 't': expectLiteral("true"); return JsonBoolean.TRUE;
                case 'f': expectLiteral("false"); return JsonBoolean.FALSE;
                case 'n': expectLiteral("null"); return JsonNull.INSTANCE;
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                    throw error("Unexpected character '" + (char) c + "'");
            }
        }

        private JsonObject parseObject() throws IOException {
            JsonObject obj = new JsonObject();
            expect('{');
            skipWhitespace();
            if (peekIs('}')) { read(); return obj; }
            while (true) {
                skipWhitespace();
                if (peek() != '"') throw error("Expected a quoted string key");
                String key = parseString();
                skipWhitespace();
                expect(':');
                obj.put(key, parseValue());
                skipWhitespace();
                int c = read();
                if (c == ',') continue;
                if (c == '}') break;
                throw error("Expected ',' or '}' but found '" + describe(c) + "'");
            }
            return obj;
        }

        private JsonArray parseArray() throws IOException {
            JsonArray arr = new JsonArray();
            expect('[');
            skipWhitespace();
            if (peekIs(']')) { read(); return arr; }
            while (true) {
                arr.add(parseValue());
                skipWhitespace();
                int c = read();
                if (c == ',') continue;
                if (c == ']') break;
                throw error("Expected ',' or ']' but found '" + describe(c) + "'");
            }
            return arr;
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                int c = read();
                if (c == -1) throw error("Unterminated string, missing closing quote");
                if (c == '"') break;
                if (c == '\\') {
                    int esc = read();
                    if (esc == -1) throw error("Incomplete escape sequence");
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': {
                            char[] hex = new char[4];
                            for (int i = 0; i < 4; i++) {
                                int h = read();
                                if (h == -1) throw error("\\u escape requires 4 hex digits");
                                hex[i] = (char) h;
                            }
                            try {
                                sb.append((char) Integer.parseInt(new String(hex), 16));
                            } catch (NumberFormatException e) {
                                throw error("Invalid hex digits in \\u escape: " + new String(hex));
                            }
                            break;
                        }
                        default: throw error("Unknown escape sequence: \\" + (char) esc);
                    }
                } else if (c < 0x20) {
                    throw error("Unescaped control character in string (0x" + Integer.toHexString(c) + ")");
                } else {
                    sb.append((char) c);
                }
            }
            return sb.toString();
        }

        private JsonValue parseNumber() throws IOException {
            StringBuilder sb = new StringBuilder();
            if (peekIs('-')) sb.append((char) read());

            int c = peek();
            if (c < '0' || c > '9') throw error("Invalid number: expected a digit after '-'");
            if (c == '0') {
                sb.append((char) read());
                if (peek() >= '0' && peek() <= '9') throw error("Invalid number: leading zero is not allowed");
            } else {
                while ((c = peek()) >= '0' && c <= '9') sb.append((char) read());
            }

            if (peekIs('.')) {
                sb.append((char) read());
                c = peek();
                if (c < '0' || c > '9') throw error("Invalid number: expected a digit after decimal point");
                while ((c = peek()) >= '0' && c <= '9') sb.append((char) read());
            }

            if (peekIs('e') || peekIs('E')) {
                sb.append((char) read());
                if (peekIs('+') || peekIs('-')) sb.append((char) read());
                c = peek();
                if (c < '0' || c > '9') throw error("Invalid number: expected a digit in exponent");
                while ((c = peek()) >= '0' && c <= '9') sb.append((char) read());
            }

            return new JsonNumber(new BigDecimal(sb.toString()));
        }

        private void expectLiteral(String literal) throws IOException {
            for (int i = 0; i < literal.length(); i++) {
                int c = read();
                if (c != literal.charAt(i)) {
                    throw error("Expected literal '" + literal + "'");
                }
            }
        }

        void expect(char c) throws IOException {
            int actual = read();
            if (actual != c) throw error("Expected '" + c + "' but found '" + describe(actual) + "'");
        }

        private boolean peekIs(char c) throws IOException {
            return peek() == c;
        }

        int peek() throws IOException {
            int c = read();
            if (c != -1) unread(c);
            return c;
        }

        int read() throws IOException {
            int c = in.read();
            if (c != -1) {
                pos++;
                if (c == '\n') { line++; col = 1; } else { col++; }
            }
            return c;
        }

        private void unread(int c) throws IOException {
            in.unread(c);
            pos--;
            if (c == '\n') line--; else col--;
        }

        void skipWhitespace() throws IOException {
            int c;
            while ((c = read()) != -1) {
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
                unread(c);
                return;
            }
        }

        JsonParseException error(String message) {
            return new JsonParseException(message, line, col, pos);
        }

        /**
         * Closes the underlying reader.
         *
         * @throws java.io.IOException if closing the reader fails
         */
        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
