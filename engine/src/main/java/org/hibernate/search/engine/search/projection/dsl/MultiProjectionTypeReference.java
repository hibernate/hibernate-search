/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.dsl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.hibernate.search.engine.search.projection.dsl.impl.ArrayMultiProjectionTypeReference;
import org.hibernate.search.engine.search.projection.dsl.impl.ListMultiProjectionTypeReference;
import org.hibernate.search.engine.search.projection.dsl.impl.SetMultiProjectionTypeReference;
import org.hibernate.search.engine.search.projection.dsl.impl.SortedSetMultiProjectionTypeReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Defines the projection collection type to use for a multi-valued fields.
 * <p>
 * Either one of the built-in type references, provided by this interface, can be used,
 * or a custom one can be implemented for other collection types.
 * @param <C> The type of the collection.
 * @param <V> The type of the elements in the collection.
 */
@Incubating
public interface MultiProjectionTypeReference<C, V> {

	/**
	 * Converts the final list of collected results to a requested collection.
	 *
	 * @param list The list of all values collected for a particular projected multi-valued field.
	 * @return The final collection containing the values of a multi-valued projection.
	 */
	C convert(List<V> list);

	default C empty() {
		return convert( List.of() );
	}

	@Override
	String toString();

	/**
	 * @return The type reference for the multi-valued projections that have to be collected as {@code List<V>}.
	 * @param <V> The type of the elements in the collection.
	 */
	static <V> MultiProjectionTypeReference<List<V>, V> list() {
		return ListMultiProjectionTypeReference.instance();
	}

	/**
	 * @return The type reference for the multi-valued projections that have to be collected as {@code Set<V>}.
	 * @param <V> The type of the elements in the collection.
	 */
	static <V> MultiProjectionTypeReference<Set<V>, V> set() {
		return SetMultiProjectionTypeReference.instance();
	}

	/**
	 * @return The type reference for the multi-valued projections that have to be collected as {@code SortedSet<V>}.
	 * @param <V> The type of the elements in the collection.
	 */
	static <V> MultiProjectionTypeReference<SortedSet<V>, V> sortedSet() {
		return SortedSetMultiProjectionTypeReference.instance();
	}

	/**
	 * @return The type reference for the multi-valued projections that have to be collected as {@code SortedSet<V>} with a custom comparator.
	 * @param <V> The type of the elements in the collection.
	 */
	static <V> MultiProjectionTypeReference<SortedSet<V>, V> sortedSet(Comparator<V> comparator) {
		return SortedSetMultiProjectionTypeReference.instance( comparator );
	}

	/**
	 * @return The type reference for the multi-valued projections that have to be collected as {@code V[]}.
	 * @param <V> The type of the elements in the collection.
	 */
	static <V> MultiProjectionTypeReference<V[], V> array(Class<V> elementType) {
		return ArrayMultiProjectionTypeReference.instance( elementType );
	}

}
