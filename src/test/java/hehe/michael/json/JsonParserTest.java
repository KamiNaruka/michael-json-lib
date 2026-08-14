package hehe.michael.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    @Test
    void parsesPrimitives() {
        assertEquals(JsonNull.INSTANCE, Json.parse("null"));
        assertTrue(Json.parse("true").asBoolean());
        assertFalse(Json.parse("false").asBoolean());
        assertEquals(42, Json.parse("42").asInt());
        assertEquals(3.14, Json.parse("3.14").asDouble(), 0.0001);
        assertEquals("hello", Json.parse("\"hello\"").asString());
    }

    @Test
    void parsesObject() {
        JsonValue v = Json.parse("{\"name\":\"Michael\",\"age\":30,\"active\":true}");
        assertTrue(v.isObject());
        assertEquals("Michael", v.get("name").asString());
        assertEquals(30, v.get("age").asInt());
        assertTrue(v.get("active").asBoolean());
    }

    @Test
    void parsesNestedStructures() {
        String json = "{\"users\":[{\"id\":1,\"tags\":[\"a\",\"b\"]},{\"id\":2,\"tags\":[]}]}";
        JsonValue v = Json.parse(json);
        assertEquals(2, v.get("users").asArray().size());
        assertEquals(1, v.get("users").get(0).get("id").asInt());
        assertEquals("a", v.get("users").get(0).get("tags").get(0).asString());
        assertEquals(0, v.get("users").get(1).get("tags").asArray().size());
    }

    @Test
    void parsesEscapedStrings() {
        JsonValue v = Json.parse("\"line1\\nline2\\t\\u0041\"");
        assertEquals("line1\nline2\tA", v.asString());
    }

    @Test
    void parsesNumberEdgeCases() {
        assertEquals(0, Json.parse("0").asInt());
        assertEquals(-5, Json.parse("-5").asInt());
        assertEquals(1.5e10, Json.parse("1.5e10").asDouble(), 1);
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(JsonParseException.class, () -> Json.parse("{"));
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\":}"));
        assertThrows(JsonParseException.class, () -> Json.parse("01"));
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\":1,}"));
        assertThrows(JsonParseException.class, () -> Json.parse("nul"));
    }

    @Test
    void pathNavigationWorks() {
        JsonValue v = Json.parse("{\"a\":{\"b\":{\"c\":[10,20,30]}}}");
        assertEquals(20, v.path("a.b.c.1").asInt());
        assertTrue(v.path("a.x.y").isNull());
    }
}
