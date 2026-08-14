package heehee.michael.json.bind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as bound to a specific JSON member name, and optionally
 * requires that the member be present when deserializing.
 *
 * <pre>{@code
 * public class User {
 *     @JsonProperty("full_name")
 *     private String name;
 *
 *     @JsonProperty(value = "e", required = true)
 *     private String email;
 * }
 * }</pre>
 *
 * <p>If {@link #value()} is left blank, the Java field name is used as-is
 * as the JSON member name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonProperty {

    /** The JSON member name to bind this field to. Defaults to the field's own name. */
    String value() default "";

    /**
     * If {@code true}, {@link JsonMapper#fromJson} throws a {@link JsonBindException}
     * when the JSON object being deserialized is missing this member.
     */
    boolean required() default false;
}
