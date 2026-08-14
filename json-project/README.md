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

## Project layout

```
json/
├── build.gradle.kts
├── settings.gradle.kts
├── src/main/java/hehe/michael/json/
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
│   └── JsonTypeException.java
└── src/test/java/hehe/michael/json/
    ├── JsonParserTest.java
    ├── JsonWriterTest.java
    └── JsonValueTest.java
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
import hehe.michael.json.*;

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

## Why BigDecimal

Large integers (e.g. database IDs) lose precision as `double`. This library stores numbers
as `BigDecimal` internally and converts to `int`/`long`/`double` on demand, so no data is lost.
