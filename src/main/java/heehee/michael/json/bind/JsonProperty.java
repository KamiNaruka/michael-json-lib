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

    /**
     * Returns the JSON member name to bind this field to.
     *
     * @return configured JSON member name, or an empty string to use the Java field name
     */
    String value() default "";

    /**
     * Indicates whether the JSON member must be present while deserializing.
     *
     * @return {@code true} when a missing member should cause {@link JsonBindException}
     */
    boolean required() default false;
}
