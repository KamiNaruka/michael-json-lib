package heehee.michael.json.patch;

import heehee.michael.json.Json;
import heehee.michael.json.JsonNumber;
import heehee.michael.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonPatchTest {

    private static void assertPatchApplies(String sourceJson, String patchJson, String expectedJson) {
        JsonValue result = JsonPatch.fromJson(patchJson).apply(Json.parse(sourceJson));
        assertEquals(Json.parse(expectedJson), result);
    }

    // The following each mirror one worked example from RFC 6902 appendix A.

    @Test
    void addingAnObjectMember() {
        assertPatchApplies(
                "{\"foo\": \"bar\"}",
                "[{\"op\": \"add\", \"path\": \"/baz\", \"value\": \"qux\"}]",
                "{\"baz\": \"qux\", \"foo\": \"bar\"}");
    }

    @Test
    void addingAnArrayElement() {
        assertPatchApplies(
                "{\"foo\": [\"bar\", \"baz\"]}",
                "[{\"op\": \"add\", \"path\": \"/foo/1\", \"value\": \"qux\"}]",
                "{\"foo\": [\"bar\", \"qux\", \"baz\"]}");
    }

    @Test
    void removingAnObjectMember() {
        assertPatchApplies(
                "{\"baz\": \"qux\", \"foo\": \"bar\"}",
                "[{\"op\": \"remove\", \"path\": \"/baz\"}]",
                "{\"foo\": \"bar\"}");
    }

    @Test
    void removingAnArrayElement() {
        assertPatchApplies(
                "{\"foo\": [\"bar\", \"qux\", \"baz\"]}",
                "[{\"op\": \"remove\", \"path\": \"/foo/1\"}]",
                "{\"foo\": [\"bar\", \"baz\"]}");
    }

    @Test
    void replacingAValue() {
        assertPatchApplies(
                "{\"baz\": \"qux\", \"foo\": \"bar\"}",
                "[{\"op\": \"replace\", \"path\": \"/baz\", \"value\": \"boo\"}]",
                "{\"baz\": \"boo\", \"foo\": \"bar\"}");
    }

    @Test
    void movingAValue() {
        assertPatchApplies(
                "{\"foo\": {\"bar\": \"baz\", \"waldo\": \"fred\"}, \"qux\": {\"corge\": \"grault\"}}",
                "[{\"op\": \"move\", \"from\": \"/foo/waldo\", \"path\": \"/qux/thud\"}]",
                "{\"foo\": {\"bar\": \"baz\"}, \"qux\": {\"corge\": \"grault\", \"thud\": \"fred\"}}");
    }

    @Test
    void movingAnArrayElement() {
        assertPatchApplies(
                "{\"foo\": [\"all\", \"grass\", \"cows\", \"eat\"]}",
                "[{\"op\": \"move\", \"from\": \"/foo/1\", \"path\": \"/foo/3\"}]",
                "{\"foo\": [\"all\", \"cows\", \"eat\", \"grass\"]}");
    }

    @Test
    void addingANestedMemberObject() {
        assertPatchApplies(
                "{\"foo\": \"bar\"}",
                "[{\"op\": \"add\", \"path\": \"/child\", \"value\": {\"grandchild\": {}}}]",
                "{\"foo\": \"bar\", \"child\": {\"grandchild\": {}}}");
    }

    @Test
    void appendingToAnArrayWithDash() {
        assertPatchApplies(
                "{\"foo\": [\"bar\"]}",
                "[{\"op\": \"add\", \"path\": \"/foo/-\", \"value\": [\"abc\", \"def\"]}]",
                "{\"foo\": [\"bar\", [\"abc\", \"def\"]]}");
    }

    @Test
    void successfulTestOpIsANoOp() {
        JsonValue doc = Json.parse("{\"a\": 1}");
        JsonPatch patch = JsonPatch.of(JsonPatchOperation.test(JsonPointer.parse("/a"), new JsonNumber(1)));
        assertEquals(doc, patch.apply(doc));
    }

    @Test
    void failingTestOpThrows() {
        JsonValue doc = Json.parse("{\"a\": 1}");
        JsonPatch patch = JsonPatch.of(JsonPatchOperation.test(JsonPointer.parse("/a"), new JsonNumber(2)));
        assertThrows(JsonPatchException.class, () -> patch.apply(doc));
    }

    @Test
    void copyDuplicatesIndependently() {
        JsonValue doc = Json.parse("{\"a\": 1, \"b\": {\"c\": 2}}");
        JsonValue copied = JsonPatch.of(JsonPatchOperation.copy(JsonPointer.parse("/b"), JsonPointer.parse("/d")))
                .apply(doc);

        assertEquals(2, copied.path("d.c").asInt());
        copied.path("d").asObject().put("c", 999);
        assertEquals(2, copied.path("b.c").asInt(), "copy must not alias the source value");
    }

    @Test
    void moveIntoOwnChildIsRejected() {
        JsonValue doc = Json.parse("{\"b\": {\"c\": 2}}");
        JsonPatch patch = JsonPatch.of(JsonPatchOperation.move(JsonPointer.parse("/b"), JsonPointer.parse("/b/c")));
        assertThrows(JsonPatchException.class, () -> patch.apply(doc));
    }

    @Test
    void applyDoesNotMutateItsInput() {
        JsonValue doc = Json.parse("{\"a\":{\"b\":1}}");
        String before = doc.toString();
        JsonPatch.of(JsonPatchOperation.replace(JsonPointer.parse("/a/b"), new JsonNumber(999))).apply(doc);
        assertEquals(before, doc.toString());
    }

    @Test
    void diffThenApplyReproducesTarget() {
        JsonValue source = Json.parse("{\"a\":1,\"b\":{\"x\":1,\"y\":2},\"c\":[1,2,3],\"keep\":true}");
        JsonValue target = Json.parse("{\"a\":1,\"b\":{\"x\":9},\"c\":[1,9,3,4],\"keep\":true,\"new\":\"hi\"}");

        JsonPatch patch = JsonPatch.diff(source, target);
        assertEquals(target, patch.apply(source));

        // wire-format round trip
        JsonPatch reparsed = JsonPatch.fromJson(patch.toJson());
        assertEquals(target, reparsed.apply(source));
    }

    @Test
    void diffHandlesArrayShrinking() {
        JsonValue source = Json.parse("[1,2,3,4,5]");
        JsonValue target = Json.parse("[1,2]");
        assertEquals(target, JsonPatch.diff(source, target).apply(source));
    }
}
