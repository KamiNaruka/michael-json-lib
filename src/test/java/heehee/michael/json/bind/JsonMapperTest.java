package heehee.michael.json.bind;

import heehee.michael.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapperTest {

    enum Role { ADMIN, MEMBER }

    static class Address {
        String city;
        String zip;

        Address() { }

        Address(String city, String zip) {
            this.city = city;
            this.zip = zip;
        }
    }

    static class User {
        @JsonProperty("full_name")
        String name;
        int age;
        Role role;
        LocalDate joined;
        Address address;
        List<String> tags;
        Map<String, Integer> scores;
        @JsonIgnore
        String secret = "should not appear";
        transient String alsoIgnored = "nope";

        User() { }

        User(String name, int age, Role role, LocalDate joined, Address address,
             List<String> tags, Map<String, Integer> scores) {
            this.name = name;
            this.age = age;
            this.role = role;
            this.joined = joined;
            this.address = address;
            this.tags = tags;
            this.scores = scores;
        }
    }

    static class RequiredHolder {
        @JsonProperty(value = "must_have", required = true)
        String value;
    }

    private final JsonMapper mapper = new JsonMapper();

    @Test
    void serializesRenamedIgnoredAndNestedFields() {
        User u = new User("Ada Lovelace", 30, Role.ADMIN, LocalDate.of(2024, 1, 15),
                new Address("London", "SW1"), List.of("math", "computing"),
                new LinkedHashMap<>(Map.of("puzzles", 99)));

        String text = mapper.toJsonString(u);

        assertFalse(text.contains("secret"), "@JsonIgnore field leaked into JSON");
        assertFalse(text.contains("alsoIgnored"), "transient field leaked into JSON");
        assertTrue(text.contains("full_name"), "@JsonProperty rename not applied");
        assertTrue(text.contains("2024-01-15"), "LocalDate not serialized as ISO-8601");
    }

    @Test
    void roundTripsThroughPojo() {
        User u = new User("Ada Lovelace", 30, Role.ADMIN, LocalDate.of(2024, 1, 15),
                new Address("London", "SW1"), List.of("math", "computing"),
                new LinkedHashMap<>(Map.of("puzzles", 99)));

        JsonValue json = mapper.toJson(u);
        User back = mapper.fromJson(json, User.class);

        assertEquals("Ada Lovelace", back.name);
        assertEquals(30, back.age);
        assertEquals(Role.ADMIN, back.role);
        assertEquals(LocalDate.of(2024, 1, 15), back.joined);
        assertEquals("London", back.address.city);
        assertEquals(List.of("math", "computing"), back.tags);
        assertEquals(99, back.scores.get("puzzles").intValue());
        assertEquals("should not appear", back.secret, "ignored field should keep its constructor default");
    }

    @Test
    void listOfPojosViaTypeReference() {
        String json = "[{\"full_name\":\"A\",\"age\":1},{\"full_name\":\"B\",\"age\":2}]";
        List<User> users = mapper.fromJson(json, new TypeReference<List<User>>() { });

        assertEquals(2, users.size());
        assertEquals("B", users.get(1).name);
    }

    @Test
    void nestedGenericsViaTypeReference() {
        Map<String, List<Integer>> nested = mapper.fromJson(
                "{\"a\":[1,2,3],\"b\":[4,5]}", new TypeReference<Map<String, List<Integer>>>() { });

        assertEquals(List.of(1, 2, 3), nested.get("a"));
        assertEquals(List.of(4, 5), nested.get("b"));
    }

    @Test
    void requiredPropertyIsEnforced() {
        assertThrows(JsonBindException.class, () -> mapper.fromJson("{}", RequiredHolder.class));
    }

    @Test
    void primitiveArraysWork() {
        int[] arr = mapper.fromJson("[1,2,3]", int[].class);
        assertArrayEquals(new int[] { 1, 2, 3 }, arr);
    }

    @Test
    void untypedObjectFallsBackToMapsAndLists() {
        Object plain = mapper.fromJson("{\"x\":[1,2,{\"y\":true}]}", Object.class);
        assertInstanceOf(Map.class, plain);
    }
}
