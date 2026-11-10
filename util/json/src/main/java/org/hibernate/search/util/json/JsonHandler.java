package org.hibernate.search.util.json;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Converts a JSON library's tree/value representation to and from strict JSON text.
 * <p>
 * {@code T} represents JSON values, not entities or arbitrary application DTOs.
 * Implementations must preserve arbitrary members and nested JSON null values.
 * Both methods use exactly one strict JSON value. The configuration and runtime type token must be stable,
 * and methods must support concurrent invocation. Parsed values must be independent.
 * <p>
 * When declaring compatible native fields with the same path in multiple indexes,
 * reuse the same handler instance. Hibernate Search compares their handlers by identity,
 * not by {@link Object#equals(Object)}; separately created handlers are not considered compatible
 * even if their types and conversion logic are equivalent. The Java type and other field configuration
 * must also be compatible. Custom implementations should provide a shared instance when used this way.
 * <p>
 * Keep input values stable while Search uses them, including execution/retries.
 * Returned values are borrowed and read-only. After a transformer returns a tree,
 * do not mutate that tree or any subtree shared with it.
 *
 * @param <T> The JSON library's tree/value type.
 */
@Incubating
public interface JsonHandler<T> {
	/**
	 * @return The non-null, stable runtime class of every accepted/returned non-null value.
	 */
	Class<T> javaType();

	/**
	 * @param value A non-null JSON value.
	 * @return One strict JSON value as non-null text.
	 */
	String toJson(T value);

	/**
	 * @param json Non-null text containing one strict JSON value.
	 * @return An independent non-null value.
	 */
	T fromJson(String json);
}
