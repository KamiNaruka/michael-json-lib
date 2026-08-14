package heehee.michael.json.stream;

import heehee.michael.json.Json;
import heehee.michael.json.JsonValue;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Writes JSON incrementally to a {@link Writer} without ever holding a whole document
 * in memory.
 *
 * <p>Repeated calls to {@link #writeValue(JsonValue)} write one compact JSON value per
 * line, which is exactly NDJSON. For a single huge top-level array, {@link #beginArray()}
 * returns an {@link ArrayWriter} that streams each element straight to the underlying
 * {@link Writer} as it's produced, so the array is never built up in memory.
 *
 * <p>Not thread-safe; close with {@link #close()} (or try-with-resources) when done.
 */
public final class JsonStreamWriter implements Closeable, Flushable {

    private final Writer out;
    private boolean wroteSomething = false;
    private boolean arrayOpen = false;

    /**
     * Creates a streaming writer over the supplied character destination. Closing this object closes the supplied writer.
     *
     * @param out character destination
     */
    public JsonStreamWriter(Writer out) {
        this.out = (out instanceof BufferedWriter) ? out : new BufferedWriter(out);
    }

    /**
     * Opens a file for low-memory writing, encoded as UTF-8.
     *
     * @param path file to create or truncate
     * @return stream writer for the file
     * @throws IOException if the file cannot be opened for writing
     */
    public static JsonStreamWriter create(Path path) throws IOException {
        return new JsonStreamWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }

    /**
     * Writes one top-level JSON value in compact form. Successive calls are separated by a
     * newline, so writing values this way one after another produces valid NDJSON.
     *
     * @param value JSON value to write
     * @return this writer
     * @throws IOException if writing fails
     * @throws IllegalStateException if a streamed array is currently open
     */
    public JsonStreamWriter writeValue(JsonValue value) throws IOException {
        if (arrayOpen) throw new IllegalStateException("Cannot write a top-level value while an array is open");
        if (wroteSomething) out.write('\n');
        Json.write(value, out, false);
        wroteSomething = true;
        return this;
    }

    /**
     * Writes every value from {@code values} via {@link #writeValue(JsonValue)}, in order.
     *
     * @param values values to write
     * @return this writer
     * @throws IOException if writing fails
     */
    public JsonStreamWriter writeValues(Iterator<? extends JsonValue> values) throws IOException {
        while (values.hasNext()) writeValue(values.next());
        return this;
    }

    /**
     * Begins a top-level JSON array, returning an {@link ArrayWriter} to stream its elements
     * one at a time. Call {@link ArrayWriter#end()} (or close it) when done.
     *
     * @return writer for streaming elements into the array
     * @throws IOException if writing the opening bracket fails
     * @throws IllegalStateException if an array is already open
     */
    public ArrayWriter beginArray() throws IOException {
        if (arrayOpen) throw new IllegalStateException("A top-level array is already open");
        if (wroteSomething) out.write('\n');
        out.write('[');
        wroteSomething = true;
        arrayOpen = true;
        return new ArrayWriter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Closes the underlying writer. A top-level array must be ended before this writer can be closed.
     *
     * @throws IllegalStateException if an array is still open
     * @throws java.io.IOException if closing the writer fails
     */
    @Override
    public void close() throws IOException {
        if (arrayOpen) throw new IllegalStateException("Cannot close JsonStreamWriter while an array is still open");
        out.close();
    }

    /** Streams the elements of one top-level JSON array directly to the underlying writer. */
    public final class ArrayWriter implements Closeable {
        private boolean first = true;
        private boolean closed = false;

        /**
         * Writes one compact element to the open top-level array.
         *
         * @param element array element
         * @return this array writer
         * @throws java.io.IOException if writing fails
         * @throws IllegalStateException if this array writer has already been closed
         */
        public ArrayWriter write(JsonValue element) throws IOException {
            if (closed) throw new IllegalStateException("This array has already been closed with end()");
            if (!first) out.write(',');
            Json.write(element, out, false);
            first = false;
            return this;
        }

        /**
         * Writes every remaining element from an iterator to the open array.
         *
         * @param elements elements to write
         * @return this array writer
         * @throws java.io.IOException if writing fails
         */
        public ArrayWriter writeAll(Iterator<? extends JsonValue> elements) throws IOException {
            while (elements.hasNext()) write(elements.next());
            return this;
        }

        /**
         * Writes the closing {@code ']'}. Calling this more than once is a harmless no-op.
         *
         * @throws IOException if writing the closing bracket fails
         */
        public void end() throws IOException {
            if (closed) return;
            out.write(']');
            closed = true;
            arrayOpen = false;
        }

        /**
         * Closes the underlying writer. A top-level array must be ended before this writer can be closed.
         *
         * @throws IllegalStateException if an array is still open
         * @throws java.io.IOException if closing the writer fails
         */
        @Override
        public void close() throws IOException {
            end();
        }
    }
}
