package heehee.michael.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonWriterTest {

    @Test
    void stringifiesCompact() {
        JsonObject obj = Json.object().put("name", "Mike").put("age", 25).put("ok", true);
        assertEquals("{\"name\":\"Mike\",\"age\":25,\"ok\":true}", Json.stringify(obj));
    }

    @Test
    void stringifiesPretty() {
        JsonObject obj = Json.object().put("a", 1);
        String pretty = Json.stringifyPretty(obj);
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("  \"a\": 1"));
    }

    @Test
    void escapesSpecialCharactersInStrings() {
        JsonValue v = new JsonString("line1\nline2\t\"quoted\"");
        String s = Json.stringify(v);
        assertEquals("\"line1\\nline2\\t\\\"quoted\\\"\"", s);
    }

    @Test
    void roundTripsThroughParseAndStringify() {
        String original = "{\"a\":1,\"b\":[1,2,3],\"c\":{\"d\":true,\"e\":null}}";
        JsonValue parsed = Json.parse(original);
        String out = Json.stringify(parsed);
        JsonValue reparsed = Json.parse(out);
        assertEquals(parsed, reparsed);
    }

    @Test
    void numbersDropUnnecessaryTrailingZeros() {
        JsonValue v = new JsonNumber(new java.math.BigDecimal("1.50"));
        assertEquals("1.5", Json.stringify(v));
    }
}
