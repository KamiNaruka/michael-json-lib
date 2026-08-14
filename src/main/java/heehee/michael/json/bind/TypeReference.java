package heehee.michael.json.bind;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Captures a fully-parameterized generic type (e.g. {@code List<Order>} or
 * {@code Map<String, List<Order>>}) so {@link JsonMapper} can deserialize into it,
 * working around Java's type erasure.
 *
 * <p>Use it as an anonymous subclass at the call site:
 * <pre>{@code
 * List<Order> orders = mapper.fromJson(json, new TypeReference<List<Order>>() {});
 * }</pre>
 *
 * The empty {@code {}} body is required &mdash; it is what lets this class
 * inspect its own generic superclass to recover the {@code List<Order>} type
 * argument at runtime.
 */
public abstract class TypeReference<T> {

    private final Type type;

    /**
     * Captures the actual generic type argument from the anonymous subclass.
     *
     * @throws IllegalStateException if instantiated without a parameterized anonymous subclass
     */
    protected TypeReference() {
        Type superclass = getClass().getGenericSuperclass();
        if (!(superclass instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "TypeReference must be constructed as an anonymous subclass with an actual "
                            + "type argument, e.g. `new TypeReference<List<Order>>() {}`");
        }
        this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }

    /**
     * Returns the captured runtime type.
     *
     * @return captured type
     */
    public Type getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return type.toString();
    }
}
