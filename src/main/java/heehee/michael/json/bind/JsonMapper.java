package heehee.michael.json.bind;

import heehee.michael.json.Json;
import heehee.michael.json.JsonArray;
import heehee.michael.json.JsonBoolean;
import heehee.michael.json.JsonNull;
import heehee.michael.json.JsonNumber;
import heehee.michael.json.JsonObject;
import heehee.michael.json.JsonString;
import heehee.michael.json.JsonValue;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Converts plain Java objects (POJOs) to and from the {@link JsonValue} tree.
 *
 * <p>Fields are bound by name (no getters/setters required). A field's JSON member
 * name defaults to the field's own name, and can be overridden with
 * {@link JsonProperty @JsonProperty("...")}; fields marked {@link JsonIgnore @JsonIgnore},
 * {@code static}, or {@code transient} are skipped entirely.
 *
 * <p>Supported field types: primitives and their wrappers, {@link String}, {@link Character},
 * {@link BigDecimal}/{@link BigInteger}, {@code enum}s (by {@link Enum#name()}), the common
 * {@code java.time} types ({@link LocalDate}, {@link LocalDateTime}, {@link LocalTime},
 * {@link Instant}, {@link ZonedDateTime}, {@link OffsetDateTime}, {@link Duration}, {@link Period}
 * &mdash; all via their ISO-8601 {@code toString()}/{@code parse()}), nested POJOs, Java arrays,
 * {@link Collection}s, and {@link Map}s (with {@link String} keys). Generic container types at
 * the top level (e.g. {@code List<Order>}) need a {@link TypeReference} since Java erases them.
 *
 * <p>POJOs need a no-arg constructor (of any visibility). This class is thread-safe and holds
 * no per-call state, so a single instance can be reused/shared freely.
 */
public final class JsonMapper {

    public JsonMapper() {
    }

    // ------------------------------------------------------------------
    // Java object -> JsonValue
    // ------------------------------------------------------------------

    /** Converts an arbitrary Java object into a {@link JsonValue} tree. */
    public JsonValue toJson(Object value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof JsonValue jv) return jv;
        if (value instanceof String s) return new JsonString(s);
        if (value instanceof Character c) return new JsonString(c.toString());
        if (value instanceof Boolean b) return JsonBoolean.of(b);
        if (value instanceof BigDecimal bd) return new JsonNumber(bd);
        if (value instanceof BigInteger bi) return new JsonNumber(new BigDecimal(bi));
        if (value instanceof Double || value instanceof Float) return numberFromFloating((Number) value);
        if (value instanceof Number n) return new JsonNumber(new BigDecimal(n.toString()));
        if (value instanceof Enum<?> e) return new JsonString(e.name());
        if (isTemporalLike(value)) return new JsonString(value.toString());
        if (value instanceof Map<?, ?> map) return mapToJson(map);
        if (value instanceof Collection<?> collection) return collectionToJson(collection);
        if (value.getClass().isArray()) return arrayToJson(value);
        return pojoToJson(value);
    }

    public String toJsonString(Object value) {
        return Json.stringify(toJson(value));
    }

    public String toJsonStringPretty(Object value) {
        return Json.stringifyPretty(toJson(value));
    }

    private static JsonNumber numberFromFloating(Number n) {
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new JsonBindException("Cannot represent " + d + " as JSON (NaN/Infinity aren't valid JSON numbers)");
        }
        return new JsonNumber(BigDecimal.valueOf(d));
    }

    private static boolean isTemporalLike(Object value) {
        return value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime
                || value instanceof Instant || value instanceof ZonedDateTime || value instanceof OffsetDateTime
                || value instanceof Duration || value instanceof Period;
    }

    private JsonObject mapToJson(Map<?, ?> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                throw new JsonBindException("JSON object map keys must be non-null String values, got: " + e.getKey());
            }
            obj.put(key, toJson(e.getValue()));
        }
        return obj;
    }

    private JsonArray collectionToJson(Collection<?> collection) {
        JsonArray arr = new JsonArray();
        for (Object e : collection) arr.add(toJson(e));
        return arr;
    }

    private JsonArray arrayToJson(Object array) {
        JsonArray arr = new JsonArray();
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) arr.add(toJson(Array.get(array, i)));
        return arr;
    }

    private JsonObject pojoToJson(Object value) {
        JsonObject obj = new JsonObject();
        Set<String> written = new HashSet<>();
        for (Class<?> c = value.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (skipField(field)) continue;
                String name = resolveName(field);
                if (!written.add(name)) continue; // a subclass field of the same name already won
                try {
                    field.setAccessible(true);
                    obj.put(name, toJson(field.get(value)));
                } catch (IllegalAccessException e) {
                    throw new JsonBindException(
                            "Cannot read field '" + field.getName() + "' on " + value.getClass().getName(), e);
                }
            }
        }
        return obj;
    }

    // ------------------------------------------------------------------
    // JsonValue -> Java object
    // ------------------------------------------------------------------

    public <T> T fromJson(String json, Class<T> type) {
        return fromJson(Json.parse(json), type);
    }

    public <T> T fromJson(String json, TypeReference<T> typeRef) {
        return fromJson(Json.parse(json), typeRef);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonValue json, Class<T> type) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(type, "type");
        return (T) fromJsonValue(json, type);
    }

    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonValue json, TypeReference<T> typeRef) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(typeRef, "typeRef");
        return (T) fromJsonValue(json, typeRef.getType());
    }

    private Object fromJsonValue(JsonValue json, Type type) {
        if (type == Object.class) return toPlainObject(json);

        Class<?> rawType = rawTypeOf(type);

        if (JsonValue.class.isAssignableFrom(rawType)) {
            if (!rawType.isInstance(json)) {
                throw new JsonBindException("Cannot bind a JSON " + json.type() + " to " + rawType.getName());
            }
            return json;
        }

        if (json.isNull()) {
            if (rawType.isPrimitive()) {
                throw new JsonBindException("Cannot bind JSON null to primitive type " + rawType.getName());
            }
            return null;
        }

        if (rawType == String.class) return json.asString();
        if (rawType == boolean.class || rawType == Boolean.class) return json.asBoolean();
        if (rawType == char.class || rawType == Character.class) return singleChar(json.asString());
        if (isNumericType(rawType)) return convertNumber(json.asBigDecimal(), rawType);
        if (rawType.isEnum()) return enumValue(rawType, json.asString());

        if (rawType == LocalDate.class) return LocalDate.parse(json.asString());
        if (rawType == LocalDateTime.class) return LocalDateTime.parse(json.asString());
        if (rawType == LocalTime.class) return LocalTime.parse(json.asString());
        if (rawType == Instant.class) return Instant.parse(json.asString());
        if (rawType == ZonedDateTime.class) return ZonedDateTime.parse(json.asString());
        if (rawType == OffsetDateTime.class) return OffsetDateTime.parse(json.asString());
        if (rawType == Duration.class) return Duration.parse(json.asString());
        if (rawType == Period.class) return Period.parse(json.asString());

        if (Map.class.isAssignableFrom(rawType)) return fromJsonMap(json, type, rawType);
        if (Collection.class.isAssignableFrom(rawType)) return fromJsonCollection(json, type, rawType);
        if (rawType.isArray()) return fromJsonArray(json, rawType);

        return fromJsonPojo(json, rawType);
    }

    private static char singleChar(String s) {
        if (s.length() != 1) {
            throw new JsonBindException("Expected a single-character string but got \"" + s + "\"");
        }
        return s.charAt(0);
    }

    private static boolean isNumericType(Class<?> rawType) {
        return Number.class.isAssignableFrom(rawType)
                || rawType == int.class || rawType == long.class || rawType == short.class
                || rawType == byte.class || rawType == double.class || rawType == float.class;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> rawType, String name) {
        try {
            return Enum.valueOf((Class<? extends Enum>) rawType, name);
        } catch (IllegalArgumentException e) {
            throw new JsonBindException("\"" + name + "\" is not a constant of enum " + rawType.getName(), e);
        }
    }

    private static Object convertNumber(BigDecimal bd, Class<?> rawType) {
        try {
            if (rawType == int.class || rawType == Integer.class) return bd.intValueExact();
            if (rawType == long.class || rawType == Long.class) return bd.longValueExact();
            if (rawType == short.class || rawType == Short.class) return bd.shortValueExact();
            if (rawType == byte.class || rawType == Byte.class) return bd.byteValueExact();
            if (rawType == double.class || rawType == Double.class) {
                double d = bd.doubleValue();
                if (!Double.isFinite(d)) throw new ArithmeticException("overflow");
                return d;
            }
            if (rawType == float.class || rawType == Float.class) {
                float f = bd.floatValue();
                if (!Float.isFinite(f)) throw new ArithmeticException("overflow");
                return f;
            }
            if (rawType == BigDecimal.class) return bd;
            if (rawType == BigInteger.class) return bd.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new JsonBindException(bd.toPlainString() + " doesn't fit exactly in " + rawType.getSimpleName(), e);
        }
        throw new JsonBindException("Unsupported numeric type: " + rawType.getName());
    }

    private Object fromJsonMap(JsonValue json, Type type, Class<?> rawType) {
        if (!json.isObject()) {
            throw new JsonBindException("Expected a JSON object to bind to " + rawType.getName() + " but found " + json.type());
        }
        Type valueType = Object.class;
        if (type instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 2) {
            valueType = pt.getActualTypeArguments()[1]; // JSON object keys are always strings
        }
        Map<String, Object> map = instantiateMap(rawType);
        for (Map.Entry<String, JsonValue> e : json.asObject()) {
            map.put(e.getKey(), fromJsonValue(e.getValue(), valueType));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> instantiateMap(Class<?> rawType) {
        if (rawType.isInterface()) return new LinkedHashMap<>();
        try {
            Constructor<?> ctor = rawType.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Map<String, Object>) ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JsonBindException("Cannot instantiate " + rawType.getName() + " (needs a no-arg constructor)", e);
        }
    }

    private Object fromJsonCollection(JsonValue json, Type type, Class<?> rawType) {
        if (!json.isArray()) {
            throw new JsonBindException("Expected a JSON array to bind to " + rawType.getName() + " but found " + json.type());
        }
        Type elementType = Object.class;
        if (type instanceof ParameterizedType pt && pt.getActualTypeArguments().length == 1) {
            elementType = pt.getActualTypeArguments()[0];
        }
        Collection<Object> collection = instantiateCollection(rawType);
        for (JsonValue v : json.asArray()) {
            collection.add(fromJsonValue(v, elementType));
        }
        return collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> instantiateCollection(Class<?> rawType) {
        if (rawType.isInterface()) {
            return Set.class.isAssignableFrom(rawType) ? new LinkedHashSet<>() : new ArrayList<>();
        }
        try {
            Constructor<?> ctor = rawType.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Collection<Object>) ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JsonBindException("Cannot instantiate " + rawType.getName() + " (needs a no-arg constructor)", e);
        }
    }

    private Object fromJsonArray(JsonValue json, Class<?> rawType) {
        if (!json.isArray()) {
            throw new JsonBindException("Expected a JSON array to bind to " + rawType.getName() + " but found " + json.type());
        }
        JsonArray arr = json.asArray();
        Class<?> componentType = rawType.getComponentType();
        Object result = Array.newInstance(componentType, arr.size());
        for (int i = 0; i < arr.size(); i++) {
            Array.set(result, i, fromJsonValue(arr.get(i), componentType));
        }
        return result;
    }

    private Object fromJsonPojo(JsonValue json, Class<?> rawType) {
        if (!json.isObject()) {
            throw new JsonBindException("Expected a JSON object to bind to " + rawType.getName() + " but found " + json.type());
        }
        Object instance = instantiate(rawType);
        JsonObject obj = json.asObject();
        Set<String> bound = new HashSet<>();
        for (Class<?> c = rawType; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (skipField(field)) continue;
                String name = resolveName(field);
                if (!bound.add(name)) continue; // a subclass field of the same name already won
                if (!obj.containsKey(name)) {
                    JsonProperty ann = field.getAnnotation(JsonProperty.class);
                    if (ann != null && ann.required()) {
                        throw new JsonBindException("Missing required property \"" + name + "\" for " + rawType.getName());
                    }
                    continue;
                }
                Object value = fromJsonValue(obj.get(name), field.getGenericType());
                try {
                    field.setAccessible(true);
                    field.set(instance, value);
                } catch (IllegalAccessException e) {
                    throw new JsonBindException(
                            "Cannot set field '" + field.getName() + "' on " + rawType.getName(), e);
                }
            }
        }
        return instance;
    }

    private static Object instantiate(Class<?> rawType) {
        try {
            Constructor<?> ctor = rawType.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new JsonBindException(rawType.getName() + " needs a no-arg constructor to be deserialized", e);
        } catch (ReflectiveOperationException e) {
            throw new JsonBindException("Failed to instantiate " + rawType.getName(), e);
        }
    }

    private static boolean skipField(Field field) {
        int mods = field.getModifiers();
        return Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic()
                || field.isAnnotationPresent(JsonIgnore.class);
    }

    private static String resolveName(Field field) {
        JsonProperty ann = field.getAnnotation(JsonProperty.class);
        return (ann != null && !ann.value().isEmpty()) ? ann.value() : field.getName();
    }

    private static Class<?> rawTypeOf(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        throw new JsonBindException("Unsupported generic type: " + type);
    }

    /** Converts a {@link JsonValue} into plain {@code String}/{@code BigDecimal}/{@code Boolean}/{@code List}/{@code Map}/{@code null}. */
    private static Object toPlainObject(JsonValue v) {
        switch (v.type()) {
            case NULL: return null;
            case STRING: return v.asString();
            case NUMBER: return v.asBigDecimal();
            case BOOLEAN: return v.asBoolean();
            case ARRAY: {
                List<Object> list = new ArrayList<>();
                for (JsonValue e : v.asArray()) list.add(toPlainObject(e));
                return list;
            }
            case OBJECT: {
                Map<String, Object> map = new LinkedHashMap<>();
                for (Map.Entry<String, JsonValue> e : v.asObject()) map.put(e.getKey(), toPlainObject(e.getValue()));
                return map;
            }
            default:
                throw new AssertionError("Unreachable: " + v.type());
        }
    }
}
