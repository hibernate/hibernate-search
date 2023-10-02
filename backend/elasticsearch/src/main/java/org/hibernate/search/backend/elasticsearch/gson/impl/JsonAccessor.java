/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.util.Optional;
import java.util.function.Supplier;

import com.google.gson.JsonObject;

/**
 * An interface that abstracts the ways of accessing values in a JSON tree.
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
	Optional<T> get(JsonObject root);

	/**
	 * Set the given value on the element this accessor points to for the given {@code root}.
	 *
	 * @param root The root to be accessed.
	 * @param newValue The value to set.
	 * @throws UnexpectedJsonElementTypeException If an element in the path has unexpected type, preventing
	 * access to the element this accessor points to.
	 */
	void set(JsonObject root, T newValue);

	/**
	 * Get the current value of the element this accessor points to for the given {@code root},
	 * creating it and setting it if it hasn't been set yet.
	 *
	 * @param root The root to be accessed.
	 * @param newValueSupplier The value to set and return if the current value hasn't been set yet.
	 * @return The current value pointed to by this accessor on the {@code root}, always non-null.
	 * @throws UnexpectedJsonElementTypeException if the element already exists and is not of the expected type,
	 * or if an element in the path has unexpected type, preventing access to the element this accessor
	 * points to.
	 */
	T getOrCreate(JsonObject root, Supplier<? extends T> newValueSupplier);

	static JsonObjectAccessor root() {
		return RootJsonAccessor.INSTANCE;
	}

}
