package hehe.michael.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonValueTest {

    @Test
    void builderApiWorks() {
        JsonObject obj = Json.object()
                .put("name", "Michael")
                .put("scores", Json.array().add(90).add(85).add(100));

        assertEquals("Michael", obj.getString("name"));
        assertEquals(3, obj.getArray("scores").size());
        assertEquals(100, obj.getArray("scores").getInt(2));
    }

    @Test
    void typeMismatchThrowsClearException() {
        JsonValue v = new JsonString("hello");
        assertThrows(JsonTypeException.class, v::asInt);
    }

    @Test
    void missingKeyReturnsJsonNullNotJavaNull() {
        JsonObject obj = Json.object();
        assertTrue(obj.get("missing").isNull());
    }

    @Test
    void defaultValueHelpersWork() {
        JsonObject obj = Json.object().put("a", 1);
        assertEquals(1, obj.getInt("a", 99));
        assertEquals(99, obj.getInt("b", 99));
    }

    @Test
    void equalityIsValueBased() {
        assertEquals(Json.parse("{\"a\":1}"), Json.parse("{\"a\":1}"));
        assertNotEquals(Json.parse("{\"a\":1}"), Json.parse("{\"a\":2}"));
    }
}
