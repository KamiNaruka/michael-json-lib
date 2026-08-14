package heehee.michael.json;

import java.math.BigDecimal;

final class JsonParser {

    private final String src;
    private int pos = 0;
    private int line = 1;
    private int col = 1;
    private int depth = 0;
    private static final int MAX_DEPTH = 512;

    JsonParser(String src) {
        this.src = src;
    }

    JsonValue parse() {
        skipWhitespace();
        JsonValue value = parseValue();
        skipWhitespace();
        if (pos != src.length()) {
            throw error("Unexpected extra data after a complete JSON value");
        }
        return value;
    }

    private JsonValue parseValue() {
        skipWhitespace();
        if (pos >= src.length()) throw error("Unexpected end of input, expected a JSON value");
        char c = src.charAt(pos);
        switch (c) {
            case '{': return parseObject();
            case '[': return parseArray();
            case '"': return new JsonString(parseString());
            case 't': return parseLiteral("true", JsonBoolean.TRUE);
            case 'f': return parseLiteral("false", JsonBoolean.FALSE);
            case 'n': return parseLiteral("null", JsonNull.INSTANCE);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) return parseNumber();
                throw error("Unexpected character '" + c + "'");
        }
    }

    private JsonObject parseObject() {
        enterContainer();
        JsonObject obj = new JsonObject();
        expect('{');
        skipWhitespace();
        if (peekIs('}')) { advance(); depth--; return obj; }
        while (true) {
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != '"') {
                throw error("Expected a quoted string key");
            }
            String key = parseString();
            skipWhitespace();
            expect(':');
            JsonValue value = parseValue();
            obj.put(key, value);
            skipWhitespace();
            if (pos >= src.length()) throw error("Unterminated object, missing '}'");
            char c = src.charAt(pos);
            if (c == ',') { advance(); continue; }
            if (c == '}') { advance(); break; }
            throw error("Expected ',' or '}' but found '" + c + "'");
        }
        depth--;
        return obj;
    }

    private JsonArray parseArray() {
        enterContainer();
        JsonArray arr = new JsonArray();
        expect('[');
        skipWhitespace();
        if (peekIs(']')) { advance(); depth--; return arr; }
        while (true) {
            JsonValue value = parseValue();
            arr.add(value);
            skipWhitespace();
            if (pos >= src.length()) throw error("Unterminated array, missing ']'");
            char c = src.charAt(pos);
            if (c == ',') { advance(); continue; }
            if (c == ']') { advance(); break; }
            throw error("Expected ',' or ']' but found '" + c + "'");
        }
        depth--;
        return arr;
    }

    private void enterContainer() {
        if (++depth > MAX_DEPTH) throw error("JSON nesting exceeds maximum depth of " + MAX_DEPTH);
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) throw error("Unterminated string, missing closing quote");
            char c = src.charAt(pos);
            advance();
            if (c == '"') break;
            if (c == '\\') {
                if (pos >= src.length()) throw error("Incomplete escape sequence");
                char esc = src.charAt(pos);
                advance();
                switch (esc) {
                    case '"':  sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'n':  sb.append('\n'); break;
                    case 'r':  sb.append('\r'); break;
                    case 't':  sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > src.length()) throw error("\\u escape requires 4 hex digits");
                        String hex = src.substring(pos, pos + 4);
                        int code;
                        try {
                            code = Integer.parseInt(hex, 16);
                        } catch (NumberFormatException e) {
                            throw error("Invalid hex digits in \\u escape: " + hex);
                        }
                        for (int i = 0; i < 4; i++) advance();
                        sb.append((char) code);
                        break;
                    default:
                        throw error("Unknown escape sequence: \\" + esc);
                }
            } else if (c < 0x20) {
                throw error("Unescaped control character in string (0x" + Integer.toHexString(c) + ")");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private JsonValue parseNumber() {
        int start = pos;
        if (peekIs('-')) advance();

        if (pos >= src.length() || !isDigit(src.charAt(pos))) {
            throw error("Invalid number: expected a digit after '-'");
        }
        if (src.charAt(pos) == '0') {
            advance();
            if (pos < src.length() && isDigit(src.charAt(pos))) {
                throw error("Invalid number: leading zero is not allowed");
            }
        } else {
            while (pos < src.length() && isDigit(src.charAt(pos))) advance();
        }

        if (pos < src.length() && src.charAt(pos) == '.') {
            advance();
            if (pos >= src.length() || !isDigit(src.charAt(pos))) {
                throw error("Invalid number: expected a digit after decimal point");
            }
            while (pos < src.length() && isDigit(src.charAt(pos))) advance();
        }

        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            advance();
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) advance();
            if (pos >= src.length() || !isDigit(src.charAt(pos))) {
                throw error("Invalid number: expected a digit in exponent");
            }
            while (pos < src.length() && isDigit(src.charAt(pos))) advance();
        }

        String numStr = src.substring(start, pos);
        try {
            return new JsonNumber(new BigDecimal(numStr));
        } catch (NumberFormatException e) {
            throw error("Invalid JSON number: " + numStr);
        }
    }

    private JsonValue parseLiteral(String literal, JsonValue result) {
        if (!src.startsWith(literal, pos)) {
            throw error("Expected literal '" + literal + "'");
        }
        for (int i = 0; i < literal.length(); i++) advance();
        return result;
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw error("Expected '" + c + "'");
        }
        advance();
    }

    private boolean peekIs(char c) {
        return pos < src.length() && src.charAt(pos) == c;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void advance() {
        if (pos < src.length()) {
            if (src.charAt(pos) == '\n') { line++; col = 1; } else { col++; }
            pos++;
        }
    }

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else {
                break;
            }
        }
    }

    private JsonParseException error(String message) {
        return new JsonParseException(message, line, col, pos);
    }
}
