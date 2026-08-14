# json

Lightweight JSON library for Java with no runtime dependencies.

[![](https://jitpack.io/v/KamiNaruka/michael-json-lib.svg)](https://jitpack.io/#KamiNaruka/michael-json-lib)

## Features

- Parse and write JSON
- `BigDecimal` numbers
- Object/array builder API
- POJO data binding
- Streaming / NDJSON
- JSON Pointer, Patch, and Merge Patch

## Build

```bash
gradle build
gradle test
```

## Usage

```java
import heehee.michael.json.*;

JsonValue json = Json.parse("""
    {"name":"Michael","age":25,"tags":["java","json"]}
    """);

String name = json.get("name").asString();
int age = json.get("age").asInt();
String tag = json.get("tags").get(0).asString();
```

```java
JsonObject json = Json.object()
        .put("name", "Michael")
        .put("age", 25)
        .put("active", true);

String text = Json.stringify(json);
String pretty = Json.stringifyPretty(json);
```

## Data binding

```java
JsonMapper mapper = new JsonMapper();

JsonValue json = mapper.toJson(user);
User user = mapper.fromJson(json, User.class);
```

## Streaming

```java
try (JsonStreamReader reader = JsonStreamReader.open(Path.of("data.ndjson"))) {
    reader.stream().forEach(System.out::println);
}
```

## JSON Patch

```java
JsonPatch patch = JsonPatch.fromJson("""
    [{"op":"replace","path":"/name","value":"Michael"}]
    """);

JsonValue result = patch.apply(json);
```

