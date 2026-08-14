package heehee.michael.json.patch;

import heehee.michael.json.Json;
import heehee.michael.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonMergePatchTest {

    private static void assertMergeApplies(String targetJson, String patchJson, String expectedJson) {
        JsonValue result = JsonMergePatch.fromJson(patchJson).apply(Json.parse(targetJson));
        assertEquals(Json.parse(expectedJson), result);
    }

    // Each of these mirrors one example from RFC 7396 section 1.

    @Test
    void replacesAScalarMember() {
        assertMergeApplies("{\"a\":\"b\"}", "{\"a\":\"c\"}", "{\"a\":\"c\"}");
    }

    @Test
    void addsANewMember() {
        assertMergeApplies("{\"a\":\"b\"}", "{\"b\":\"c\"}", "{\"a\":\"b\",\"b\":\"c\"}");
    }

    @Test
    void nullRemovesAMember() {
        assertMergeApplies("{\"a\":\"b\"}", "{\"a\":null}", "{}");
        assertMergeApplies("{\"a\":\"b\",\"b\":\"c\"}", "{\"a\":null}", "{\"b\":\"c\"}");
    }

    @Test
    void arrayIsReplacedNotMerged() {
        assertMergeApplies("{\"a\":[\"b\"]}", "{\"a\":\"c\"}", "{\"a\":\"c\"}");
        assertMergeApplies("{\"a\":\"c\"}", "{\"a\":[\"b\"]}", "{\"a\":[\"b\"]}");
        assertMergeApplies("{\"a\":[{\"b\":\"c\"}]}", "{\"a\":[1]}", "{\"a\":[1]}");
        assertMergeApplies("[\"a\",\"b\"]", "[\"c\",\"d\"]", "[\"c\",\"d\"]");
    }

    @Test
    void nestedObjectsMergeRecursively() {
        assertMergeApplies(
                "{\"a\":{\"b\":\"c\"}}",
                "{\"a\":{\"b\":\"d\",\"c\":null}}",
                "{\"a\":{\"b\":\"d\"}}");
        assertMergeApplies("{}", "{\"a\":{\"bb\":{\"ccc\":null}}}", "{\"a\":{\"bb\":{}}}");
    }

    @Test
    void nonObjectPatchReplacesWholesale() {
        assertMergeApplies("{\"a\":\"b\"}", "[\"c\"]", "[\"c\"]");
        assertMergeApplies("{\"a\":\"foo\"}", "null", "null");
        assertMergeApplies("{\"a\":\"foo\"}", "\"bar\"", "\"bar\"");
    }

    @Test
    void nonObjectTargetIsIgnoredWhenPatchIsAnObject() {
        assertMergeApplies("[1,2]", "{\"a\":\"b\",\"c\":null}", "{\"a\":\"b\"}");
    }

    @Test
    void diffThenApplyReproducesTarget() {
        JsonValue source = Json.parse("{\"a\":1,\"b\":{\"x\":1,\"y\":2},\"c\":[1,2,3]}");
        JsonValue target = Json.parse("{\"a\":1,\"b\":{\"x\":9},\"d\":\"new\"}");

        JsonMergePatch patch = JsonMergePatch.diff(source, target);
        assertEquals(target, patch.apply(source));
    }

    @Test
    void diffOfEqualDocumentsIsANoOp() {
        JsonValue source = Json.parse("{\"a\":1,\"b\":{\"x\":1,\"y\":2},\"c\":[1,2,3]}");
        JsonMergePatch patch = JsonMergePatch.diff(source, source);
        assertEquals(source, patch.apply(source));
    }

    @Test
    void diffOfJsonNullFallsBackToRemoval() {
        // Known RFC 7396 limitation: a merge patch can't express "set this member to null",
        // since null in a merge patch always means "remove". diff() falls back to removing it.
        JsonValue source = Json.parse("{\"a\":1}");
        JsonValue target = Json.parse("{\"a\":null}");
        JsonMergePatch patch = JsonMergePatch.diff(source, target);
        assertEquals(Json.parse("{}"), patch.apply(source));
    }
}
