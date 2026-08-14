package hehe.michael.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

final class JsonWriter {

    private final Appendable out;
    private final int indentSize;
    private int depth = 0;

    JsonWriter(Appendable out, int indentSize) {
        this.out = out;
        this.indentSize = indentSize;
    }

    void write(JsonValue value) throws IOException {
        value.write(this);
    }

    void writeObject(JsonObject obj) throws IOException {
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
    }

    void writeArray(JsonArray arr) throws IOException {
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
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    void writeNumber(BigDecimal value) throws IOException {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            out.append("0");
            return;
        }
        out.append(value.stripTrailingZeros().toPlainString());
    }

    void writeBoolean(boolean value) throws IOException {
        out.append(value ? "true" : "false");
    }

    void writeNull() throws IOException {
        out.append("null");
    }

    private void newlineIndent() throws IOException {
        if (indentSize <= 0) return;
        out.append('\n');
        for (int i = 0; i < depth * indentSize; i++) out.append(' ');
    }
}
