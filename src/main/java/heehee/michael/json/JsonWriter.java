package heehee.michael.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class JsonWriter {

    private static final int MAX_DEPTH = 512;
    private final Appendable out;
    private final int indentSize;
    private final Set<JsonValue> activeContainers = Collections.newSetFromMap(new IdentityHashMap<>());
    private int depth = 0;

    JsonWriter(Appendable out, int indentSize) {
        if (indentSize < 0) throw new IllegalArgumentException("indentSize must be >= 0");
        this.out = out;
        this.indentSize = indentSize;
    }

    void write(JsonValue value) throws IOException {
        if (value == null) throw new IllegalArgumentException("JSON value must not be null");
        value.write(this);
    }

    void writeObject(JsonObject obj) throws IOException {
        enter(obj);
        try {
            out.append('{');
            depth++;
            Iterator<Map.Entry<String, JsonValue>> it = obj.entrySet().iterator();
            if (it.hasNext()) {
                newlineIndent();
                while (it.hasNext()) {
                    Map.Entry<String, JsonValue> entry = it.next();
                    writeString(entry.getKey());
                    out.append(':');
                    if (indentSize > 0) out.append(' ');
                    entry.getValue().write(this);
                    if (it.hasNext()) {
                        out.append(',');
                        newlineIndent();
                    }
                }
            }
            depth--;
            if (obj.size() > 0) newlineIndent();
            out.append('}');
        } finally {
            activeContainers.remove(obj);
        }
    }

    void writeArray(JsonArray arr) throws IOException {
        enter(arr);
        try {
            out.append('[');
            depth++;
            Iterator<JsonValue> it = arr.iterator();
            if (it.hasNext()) {
                newlineIndent();
                while (it.hasNext()) {
                    JsonValue v = it.next();
                    v.write(this);
                    if (it.hasNext()) {
                        out.append(',');
                        newlineIndent();
                    }
                }
            }
            depth--;
            if (arr.size() > 0) newlineIndent();
            out.append(']');
        } finally {
            activeContainers.remove(arr);
        }
    }

    private void enter(JsonValue container) {
        if (depth >= MAX_DEPTH) throw new IllegalStateException("JSON nesting exceeds maximum depth of " + MAX_DEPTH);
        if (!activeContainers.add(container)) throw new IllegalStateException("Cyclic JsonValue graph cannot be serialized");
    }

    void writeString(String s) throws IOException {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        appendUnicodeEscape(c);
                    } else if (Character.isHighSurrogate(c)) {
                        if (i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1))) {
                            out.append(c).append(s.charAt(++i));
                        } else {
                            appendUnicodeEscape(c);
                        }
                    } else if (Character.isLowSurrogate(c)) {
                        appendUnicodeEscape(c);
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private void appendUnicodeEscape(char c) throws IOException {
        final char[] hex = "0123456789abcdef".toCharArray();
        out.append("\\u")
                .append(hex[(c >>> 12) & 0xF])
                .append(hex[(c >>> 8) & 0xF])
                .append(hex[(c >>> 4) & 0xF])
                .append(hex[c & 0xF]);
    }

    void writeNumber(BigDecimal value) throws IOException {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            out.append('0');
            return;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        // Avoid expanding inputs such as 1e100000000 into enormous strings.
        int adjustedExponent = normalized.precision() - normalized.scale() - 1;
        if (adjustedExponent < -6 || adjustedExponent > 20) out.append(normalized.toString());
        else out.append(normalized.toPlainString());
    }

    void writeBoolean(boolean value) throws IOException { out.append(value ? "true" : "false"); }
    void writeNull() throws IOException { out.append("null"); }

    private void newlineIndent() throws IOException {
        if (indentSize <= 0) return;
        out.append('\n');
        for (int i = 0; i < depth * indentSize; i++) out.append(' ');
    }
}
