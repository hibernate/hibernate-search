package org.hibernate.search.backend.elasticsearch.json.impl;

import java.util.Optional;
import java.util.function.Supplier;

import tools.jackson.databind.node.ObjectNode;


/**
 * Immutable, reusable navigation and mutation definitions for completed trees or trees under construction.
 * Typed access is strict; singleton-array adaptation requires explicit opt-in at every selected location.
 * Accessors are never used by the parser.
 *
 * @see #root()
 * @see JsonObjectAccessor
 * @see JsonArrayAccessor
 * @see UnknownTypeJsonAccessor
 *
 */
public interface JsonAccessor<T> {

	/**
	 * Get the current value of the element this accessor points to for the given {@code root}.
	 *
	 * @param root The root to be accessed.
	 * @return An {@link java.util.Optional} containing the current value pointed to by this accessor on the {@code root},
	 * or {@link java.util.Optional#empty()} if it doesn't exist.
	 * @throws UnexpectedJsonElementTypeException If an element in the path has unexpected type,
	 * preventing access to the element this accessor points to.
	 */
	Optional<T> get(ObjectNode root);

	/**
	 * Set the given value on the element this accessor points to for the given {@code root}.
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to set.
	 * @throws UnexpectedJsonElementTypeException If an element in the path has unexpected type, preventing
	 * access to the element this accessor points to.
	 */
	void set(ObjectNode root, T newValue);

	/**
	 * Get the current value of the element this accessor points to for the given {@code root},
	 * creating it and setting it if it hasn't been set yet.
	 *
	 * @param root The root to be accessed.
	 * @param newValueSupplier The value to set and return if the current value hasn't been set yet.
	 * @return The current value, or null when an existing selected singleton/empty array adapts to absence.
	 * Creation is based on raw missing/JSON-null presence: an adapted absence does not replace a wrapper.
	 * @throws IllegalStateException If traversal requires an object/array inside an absent adapted parent.
	 * @throws UnexpectedJsonElementTypeException if the element already exists and is not of the expected type,
	 * or if an element in the path has unexpected type, preventing access to the element this accessor
	 * points to.
	 */
	T getOrCreate(ObjectNode root, Supplier<? extends T> newValueSupplier);

	static JsonObjectAccessor root() {
		return RootJsonAccessor.INSTANCE;
	}

}
