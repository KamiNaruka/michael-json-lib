# json

A standalone JSON library for Java — no external dependency at runtime
(JUnit 5 is only used for tests).

- Strict RFC 8259 parser — rejects leading zeros, trailing commas, comments
- Numbers stored internally as `BigDecimal`, no precision loss on large integers
- Parse errors report line/column/offset
- Chainable builder API (`Json.object().put(...).put(...)`)
- `path("a.b.0.c")` dot-path navigation that returns `JsonNull` instead of throwing when a segment is missing
- Compact and pretty printing
- Immutable value types (`JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`) plus mutable containers (`JsonObject`, `JsonArray`)
- **Data binding** (`heehee.michael.json.bind`) — POJO ↔ JSON via `@JsonProperty`/`@JsonIgnore`, with nested objects, `List`/`Map`, enums, `java.time`, and generics through `TypeReference`
- **Streaming API** (`heehee.michael.json.stream`) — `JsonStreamReader`/`JsonStreamWriter` read/write without buffering the whole document, including NDJSON
- **JSON tooling** (`heehee.michael.json.patch`) — `JsonPointer` (RFC 6901), `JsonPatch` (RFC 6902, with diff), `JsonMergePatch` (RFC 7396, with diff)

## Project layout

```
json/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/java/heehee/michael/json/
│   ├── Json.java
│   ├── JsonValue.java
│   ├── JsonObject.java
│   ├── JsonArray.java
│   ├── JsonString.java
│   ├── JsonNumber.java
│   ├── JsonBoolean.java
│   ├── JsonNull.java
│   ├── JsonParser.java
│   ├── JsonWriter.java
│   ├── JsonType.java
│   ├── JsonParseException.java
│   ├── JsonTypeException.java
│   ├── bind/                  # POJO <-> JSON data binding
│   │   ├── JsonMapper.java
│   │   ├── JsonProperty.java
│   │   ├── JsonIgnore.java
│   │   ├── TypeReference.java
│   │   └── JsonBindException.java
│   ├── stream/                 # low-memory streaming (incl. NDJSON)
│   │   ├── JsonStreamReader.java
│   │   └── JsonStreamWriter.java
│   └── patch/                  # JSON Pointer / Patch / Merge Patch
│       ├── JsonPointer.java
│       ├── JsonPointerException.java
│       ├── JsonPatch.java
│       ├── JsonPatchOperation.java
│       ├── JsonPatchException.java
│       ├── JsonMergePatch.java
│       └── JsonCopy.java
└── src/test/java/heehee/michael/json/
    ├── JsonParserTest.java
    ├── JsonWriterTest.java
    ├── JsonValueTest.java
    ├── bind/JsonMapperTest.java
    ├── stream/JsonStreamTest.java
    └── patch/
        ├── JsonPointerTest.java
        ├── JsonPatchTest.java
        └── JsonMergePatchTest.java
```

## Build / Test

The Gradle wrapper jar is not included. If Gradle is already installed, generate it once:

```bash
gradle wrapper --gradle-version 8.10
```

Then use `./gradlew` as usual:

```bash
./gradlew build
./gradlew test
```

(Or just `gradle build` / `gradle test` directly if no wrapper is set up.)

## Usage

### Parse

```java
import heehee.michael.json.*;

JsonValue v = Json.parse("""
    {
      "name": "Michael",
      "age": 25,
      "active": true,
      "tags": ["kotlin", "java"],
      "address": { "city": "Bangkok" }
    }
    """);

String name = v.get("name").asString();
int age = v.get("age").asInt();
String city = v.path("address.city").asString();
String tag0 = v.get("tags").get(0).asString();

boolean hasEmail = !v.get("email").isNull();
```

### Build JSON

```java
JsonObject obj = Json.object()
        .put("name", "Michael")
        .put("age", 25)
        .put("active", true)
        .put("scores", Json.array().add(90).add(85).add(100));

String compact = Json.stringify(obj);
String pretty = Json.stringifyPretty(obj);
```

### Read / write files

```java
JsonValue data = Json.parseFile(Path.of("config.json"));
Json.writeToFile(data, Path.of("out.json"), true);
```

### Error handling

```java
try {
    Json.parse("{\"a\": }");
} catch (JsonParseException e) {
    System.out.println(e.getMessage());
}

try {
    v.get("age").asString();
} catch (JsonTypeException e) {
    System.out.println(e.getMessage());
}
```

## Data binding

```java
import heehee.michael.json.bind.*;

class User {
    @JsonProperty("full_name")
    String name;
    int age;
    Role role;                 // enum, bound by name()
    LocalDate joined;          // java.time, bound via ISO-8601
    List<String> tags;
    @JsonIgnore
    String internalNote;       // excluded from JSON entirely
}

JsonMapper mapper = new JsonMapper();

JsonValue json = mapper.toJson(user);
User back = mapper.fromJson(json, User.class);

// Generic types need a TypeReference, since Java erases them:
List<User> users = mapper.fromJson(jsonArrayText, new TypeReference<List<User>>() {});
Map<String, List<Integer>> nested = mapper.fromJson(text, new TypeReference<Map<String, List<Integer>>>() {});
```

POJOs need a no-arg constructor (any visibility). Fields are matched by name (or by
`@JsonProperty("...")`); `static`/`transient` fields are skipped automatically, same as `@JsonIgnore`.

## Streaming (low-memory, NDJSON)

```java
import heehee.michael.json.stream.*;

// Write NDJSON — one compact JSON value per line
try (JsonStreamWriter w = JsonStreamWriter.create(Path.of("events.ndjson"))) {
    for (JsonObject event : events) w.writeValue(event);
}

// Read it back one value at a time, never loading the whole file
try (JsonStreamReader r = JsonStreamReader.open(Path.of("events.ndjson"))) {
    r.stream().forEach(event -> process(event));
}

// Stream the elements of one huge top-level array without buffering it
try (JsonStreamReader r = JsonStreamReader.open(Path.of("big-array.json"))) {
    Iterator<JsonValue> it = r.readArrayElements();
    while (it.hasNext()) process(it.next());
}
```

## JSON Pointer / Patch / Merge Patch

```java
import heehee.michael.json.patch.*;

// RFC 6901 — JSON Pointer
JsonValue city = JsonPointer.parse("/address/city").evaluate(doc);

// RFC 6902 — JSON Patch, with diff
JsonPatch patch = JsonPatch.fromJson("""
    [{"op": "replace", "path": "/name", "value": "New Name"}]
    """);
JsonValue updated = patch.apply(doc);          // doc itself is left untouched
JsonPatch computed = JsonPatch.diff(oldDoc, newDoc);

// RFC 7396 — JSON Merge Patch, with diff
JsonMergePatch merge = JsonMergePatch.fromJson("{\"name\": \"New Name\", \"nickname\": null}");
JsonValue merged = merge.apply(doc);           // null removes a member
JsonMergePatch mergeDiff = JsonMergePatch.diff(oldDoc, newDoc);
```

## Why BigDecimal

Large integers (e.g. database IDs) lose precision as `double`. This library stores numbers
as `BigDecimal` internally and converts to `int`/`long`/`double` on demand, so no data is lost.
