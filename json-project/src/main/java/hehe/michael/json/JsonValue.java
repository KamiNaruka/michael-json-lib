package hehe.michael.json;

import java.math.BigDecimal;

public abstract class JsonValue {

    public abstract JsonType type();

    public boolean isObject()  { return type() == JsonType.OBJECT; }
    public boolean isArray()   { return type() == JsonType.ARRAY; }
    public boolean isString()  { return type() == JsonType.STRING; }
    public boolean isNumber()  { return type() == JsonType.NUMBER; }
    public boolean isBoolean() { return type() == JsonType.BOOLEAN; }
    public boolean isNull()    { return type() == JsonType.NULL; }

    public JsonObject asObject() {
        throw new JsonTypeException("Not a JSON object: " + type());
    }

    public JsonArray asArray() {
        throw new JsonTypeException("Not a JSON array: " + type());
    }

    public String asString() {
        throw new JsonTypeException("Not a JSON string: " + type());
    }

    public boolean asBoolean() {
        throw new JsonTypeException("Not a JSON boolean: " + type());
    }

    public int asInt() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    public long asLong() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    public double asDouble() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    public BigDecimal asBigDecimal() {
        throw new JsonTypeException("Not a JSON number: " + type());
    }

    public JsonValue get(String key) {
        throw new JsonTypeException("get(String) only works on a JSON object, this is: " + type());
    }

    public JsonValue get(int index) {
        throw new JsonTypeException("get(int) only works on a JSON array, this is: " + type());
    }

    public JsonValue path(String dotPath) {
        JsonValue current = this;
        for (String part : dotPath.split("\\.")) {
            if (part.isEmpty()) continue;
            if (current.isObject()) {
                current = current.asObject().get(part);
            } else if (current.isArray()) {
                JsonArray arr = current.asArray();
                try {
                    int idx = Integer.parseInt(part);
                    if (idx < 0 || idx >= arr.size()) return JsonNull.INSTANCE;
                    current = arr.get(idx);
                } catch (NumberFormatException e) {
                    return JsonNull.INSTANCE;
                }
            } else {
                return JsonNull.INSTANCE;
            }
        }
        return current;
    }

    public int size() {
        throw new JsonTypeException("size() only works on object/array, this is: " + type());
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public String toString() {
        return Json.stringify(this);
    }

    public String toPrettyString() {
        return Json.stringifyPretty(this);
    }

    abstract void write(JsonWriter writer) throws java.io.IOException;
}
