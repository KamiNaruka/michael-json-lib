package heehee.michael.json.patch;

import heehee.michael.json.JsonArray;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An RFC 6901 JSON Pointer: a sequence of reference tokens identifying a location within a
 * JSON document, e.g. {@code /users/0/address/city}.
 *
 * <p>Instances are immutable. Use {@link #parse(String)} for wire-format strings (with
 * {@code ~0}/{@code ~1} escaping already applied), or {@link #of(String...)} to build one
 * directly from raw, unescaped token strings.
 */
public final class JsonPointer {

    private static final JsonPointer ROOT = new JsonPointer(List.of());

    private final List<String> tokens;

    private JsonPointer(List<String> tokens) {
        this.tokens = List.copyOf(tokens);
    }

    /** The empty pointer, referring to the whole document. */
    public static JsonPointer root() {
        return ROOT;
    }

    /** Builds a pointer directly from raw (unescaped) reference tokens. */
    public static JsonPointer of(String... tokens) {
        return new JsonPointer(Arrays.asList(tokens));
    }

    /**
     * Parses a pointer in RFC 6901 wire format, e.g. {@code "/a~1b/0/c~0d"}. An empty string
     * parses to {@link #root()}.
     *
     * @throws JsonPointerException if non-empty and not starting with {@code '/'}
     */
    public static JsonPointer parse(String pointer) {
        Objects.requireNonNull(pointer, "pointer");
        if (pointer.isEmpty()) return ROOT;
        if (pointer.charAt(0) != '/') {
            throw new JsonPointerException("A non-empty JSON Pointer must start with '/': \"" + pointer + "\"");
        }
        String[] parts = pointer.substring(1).split("/", -1);
        List<String> toks = new ArrayList<>(parts.length);
        for (String p : parts) toks.add(unescape(p));
        return new JsonPointer(toks);
    }

    public boolean isRoot() {
        return tokens.isEmpty();
    }

    /** The pointer's reference tokens, already unescaped, as an immutable list. */
    public List<String> tokens() {
        return tokens;
    }

    /** The final reference token, e.g. {@code "city"} for {@code /address/city}. */
    public String lastToken() {
        if (isRoot()) throw new JsonPointerException("The root pointer has no last token");
        return tokens.get(tokens.size() - 1);
    }

    /** The pointer to this pointer's containing location, e.g. {@code /address} for {@code /address/city}. */
    public JsonPointer parent() {
        if (isRoot()) throw new JsonPointerException("The root pointer has no parent");
        return new JsonPointer(tokens.subList(0, tokens.size() - 1));
    }

    /** A new pointer with {@code token} appended, e.g. {@code /a}.child("b") -> {@code /a/b}. */
    public JsonPointer child(String token) {
        List<String> next = new ArrayList<>(tokens.size() + 1);
        next.addAll(tokens);
        next.add(token);
        return new JsonPointer(next);
    }

    /** A new pointer with an array index appended, e.g. {@code /a}.child(2) -> {@code /a/2}. */
    public JsonPointer child(int index) {
        if (index < 0) throw new IllegalArgumentException("JSON array index must be >= 0");
        return child(String.valueOf(index));
    }

    /** {@code true} if this pointer is a proper (strict) prefix of {@code other}. */
    public boolean isPrefixOf(JsonPointer other) {
        if (tokens.size() >= other.tokens.size()) return false;
        for (int i = 0; i < tokens.size(); i++) {
            if (!tokens.get(i).equals(other.tokens.get(i))) return false;
        }
        return true;
    }

    /**
     * Resolves this pointer against {@code document}, returning the value it identifies.
     *
     * @throws JsonPointerException if any reference token doesn't resolve (missing object
     *                              member, out-of-range or non-numeric array index, or an
     *                              attempt to navigate into a scalar)
     */
    public JsonValue evaluate(JsonValue document) {
        JsonValue current = Objects.requireNonNull(document, "document");
        for (String token : tokens) {
            if (current.isObject()) {
                JsonObject obj = current.asObject();
                if (!obj.containsKey(token)) {
                    throw new JsonPointerException("No such member \"" + token + "\" (at " + this + ")");
                }
                current = obj.get(token);
            } else if (current.isArray()) {
                JsonArray arr = current.asArray();
                if (token.equals("-")) {
                    throw new JsonPointerException("'-' does not reference an existing array element (at " + this + ")");
                }
                int idx = parseArrayIndex(token);
                if (idx < 0 || idx >= arr.size()) {
                    throw new JsonPointerException(
                            "Array index " + idx + " out of range (size=" + arr.size() + ", at " + this + ")");
                }
                current = arr.get(idx);
            } else {
                throw new JsonPointerException(
                        "Cannot navigate into a " + current.type() + " with token \"" + token + "\" (at " + this + ")");
            }
        }
        return current;
    }

    /** {@code true} if {@link #evaluate(JsonValue)} would succeed against {@code document}. */
    public boolean has(JsonValue document) {
        try {
            evaluate(document);
            return true;
        } catch (JsonPointerException e) {
            return false;
        }
    }

    private static String unescape(String token) {
        if (token.indexOf('~') < 0) return token;
        StringBuilder out = new StringBuilder(token.length());
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c != '~') {
                out.append(c);
                continue;
            }
            if (++i >= token.length()) throw new JsonPointerException("Invalid '~' escape in JSON Pointer token: " + token);
            char esc = token.charAt(i);
            if (esc == '0') out.append('~');
            else if (esc == '1') out.append('/');
            else throw new JsonPointerException("Invalid '~" + esc + "' escape in JSON Pointer token: " + token);
        }
        return out.toString();
    }

    private static int parseArrayIndex(String token) {
        if (!token.matches("0|[1-9][0-9]*")) {
            throw new JsonPointerException("Invalid array index \"" + token + "\"");
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new JsonPointerException("Array index is too large: \"" + token + "\"");
        }
    }

    private static String escape(String token) {
        if (token.indexOf('~') < 0 && token.indexOf('/') < 0) return token;
        return token.replace("~", "~0").replace("/", "~1");
    }

    @Override
    public String toString() {
        if (isRoot()) return "";
        StringBuilder sb = new StringBuilder();
        for (String t : tokens) sb.append('/').append(escape(t));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JsonPointer)) return false;
        return tokens.equals(((JsonPointer) o).tokens);
    }

    @Override
    public int hashCode() {
        return tokens.hashCode();
    }
}
