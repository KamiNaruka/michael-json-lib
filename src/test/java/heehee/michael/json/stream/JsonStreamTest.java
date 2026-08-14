package heehee.michael.json.stream;

import heehee.michael.json.Json;
import heehee.michael.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonStreamTest {

    @Test
    void writeValueProducesNdjson() throws IOException {
        StringWriter sw = new StringWriter();
        try (JsonStreamWriter w = new JsonStreamWriter(sw)) {
            for (int i = 0; i < 5; i++) {
                w.writeValue(Json.object().put("i", i));
            }
        }
        String[] lines = sw.toString().split("\n");
        assertEquals(5, lines.length);
        assertEquals(3, Json.parse(lines[3]).asObject().getInt("i"));
    }

    @Test
    void ndjsonRoundTripsThroughIteratorAndStream() throws IOException {
        String ndjson = "{\"i\":0}\n{\"i\":1}\n{\"i\":2}\n";

        List<JsonValue> values = new ArrayList<>();
        try (JsonStreamReader r = new JsonStreamReader(new StringReader(ndjson))) {
            r.iterator().forEachRemaining(values::add);
        }
        assertEquals(3, values.size());
        assertEquals(1, values.get(1).asObject().getInt("i"));

        try (JsonStreamReader r = new JsonStreamReader(new StringReader(ndjson))) {
            int sum = r.stream().mapToInt(v -> v.asObject().getInt("i")).sum();
            assertEquals(3, sum);
        }
    }

    @Test
    void largeArrayStreamsElementByElementWithoutBuffering() throws IOException {
        int n = 50_000;
        Path tmp = Files.createTempFile("jsontest-bigarray", ".json");
        try {
            try (JsonStreamWriter w = JsonStreamWriter.create(tmp)) {
                JsonStreamWriter.ArrayWriter aw = w.beginArray();
                for (int i = 0; i < n; i++) {
                    aw.write(Json.object().put("id", i));
                }
                aw.end();
            }

            long count = 0;
            int lastId = -1;
            try (JsonStreamReader r = JsonStreamReader.open(tmp)) {
                Iterator<JsonValue> it = r.readArrayElements();
                while (it.hasNext()) {
                    lastId = it.next().asObject().getInt("id");
                    count++;
                }
            }
            assertEquals(n, count);
            assertEquals(n - 1, lastId);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    void emptyArrayHasNoElements() throws IOException {
        try (JsonStreamReader r = new JsonStreamReader(new StringReader("[]"))) {
            assertFalse(r.readArrayElements().hasNext());
        }
    }

    @Test
    void nestedContainersInsideAStreamedElementParseFully() throws IOException {
        String json = "[{\"a\":[1,2,{\"b\":3}]},{\"a\":[]}]";
        try (JsonStreamReader r = new JsonStreamReader(new StringReader(json))) {
            Iterator<JsonValue> it = r.readArrayElements();
            JsonValue first = it.next();
            assertEquals(3, first.asObject().getArray("a").getObject(2).getInt("b"));
            JsonValue second = it.next();
            assertEquals(0, second.asObject().getArray("a").size());
            assertFalse(it.hasNext());
        }
    }
}
