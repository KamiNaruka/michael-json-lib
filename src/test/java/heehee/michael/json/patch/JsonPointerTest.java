package heehee.michael.json.patch;

import heehee.michael.json.Json;
import heehee.michael.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonPointerTest {

    @Test
    void escapesAndUnescapesTildeAndSlash() {
        JsonPointer p = JsonPointer.parse("/a~1b/1/c~0d");
        assertEquals(List.of("a/b", "1", "c~d"), p.tokens());
        assertEquals("/a~1b/1/c~0d", p.toString());
        assertEquals(p, JsonPointer.of("a/b", "1", "c~d"));
    }

    @Test
    void rootPointerIsEmptyStringOrExplicitRoot() {
        assertTrue(JsonPointer.root().isRoot());
        assertTrue(JsonPointer.parse("").isRoot());
    }

    @Test
    void childParentAndLastTokenNavigate() {
        JsonPointer p = JsonPointer.root().child("users").child(0).child("name");
        assertEquals("/users/0/name", p.toString());
        assertEquals("/users/0", p.parent().toString());
        assertEquals("name", p.lastToken());
    }

    @Test
    void isPrefixOfIsStrict() {
        assertTrue(JsonPointer.parse("/a").isPrefixOf(JsonPointer.parse("/a/b")));
        assertFalse(JsonPointer.parse("/a/b").isPrefixOf(JsonPointer.parse("/a")));
        assertFalse(JsonPointer.parse("/a").isPrefixOf(JsonPointer.parse("/a")));
    }

    // The reference document and expected values from RFC 6901 section 5.
    private static final String RFC_DOC = "{"
            + "\"foo\": [\"bar\", \"baz\"],"
            + "\"\": 0,"
            + "\"a/b\": 1,"
            + "\"c%d\": 2,"
            + "\"e^f\": 3,"
            + "\"g|h\": 4,"
            + "\"i\\\\j\": 5,"
            + "\"k\\\"l\": 6,"
            + "\" \": 7,"
            + "\"m~n\": 8"
            + "}";

    @Test
    void evaluatesAllRfc6901Examples() {
        JsonValue root = Json.parse(RFC_DOC);

        assertEquals(root, JsonPointer.parse("").evaluate(root));
        assertEquals(Json.parse("[\"bar\",\"baz\"]"), JsonPointer.parse("/foo").evaluate(root));
        assertEquals("bar", JsonPointer.parse("/foo/0").evaluate(root).asString());
        assertEquals(0, JsonPointer.parse("/").evaluate(root).asInt());
        assertEquals(1, JsonPointer.parse("/a~1b").evaluate(root).asInt());
        assertEquals(2, JsonPointer.parse("/c%d").evaluate(root).asInt());
        assertEquals(3, JsonPointer.parse("/e^f").evaluate(root).asInt());
        assertEquals(4, JsonPointer.parse("/g|h").evaluate(root).asInt());
        assertEquals(5, JsonPointer.parse("/i\\j").evaluate(root).asInt());
        assertEquals(6, JsonPointer.parse("/k\"l").evaluate(root).asInt());
        assertEquals(7, JsonPointer.parse("/ ").evaluate(root).asInt());
        assertEquals(8, JsonPointer.parse("/m~0n").evaluate(root).asInt());
    }

    @Test
    void dashDoesNotResolveForGet() {
        JsonValue root = Json.parse(RFC_DOC);
        assertThrows(JsonPointerException.class, () -> JsonPointer.parse("/foo/-").evaluate(root));
    }

    @Test
    void missingMemberThrows() {
        JsonValue root = Json.parse(RFC_DOC);
        assertThrows(JsonPointerException.class, () -> JsonPointer.parse("/nope").evaluate(root));
        assertFalse(JsonPointer.parse("/nope").has(root));
        assertTrue(JsonPointer.parse("/foo").has(root));
    }
}
