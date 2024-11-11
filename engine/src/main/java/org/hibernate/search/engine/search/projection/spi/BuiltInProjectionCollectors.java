/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * Provides access to built-in projection collectors.
 */
public interface BuiltInProjectionCollectors {

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionCollector.Provider<V, V> nullable() {
		return SingleValuedProjectionAccumulator.PROVIDER;
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionCollector.Provider<V, List<V>> list() {
		return ListProjectionAccumulator.PROVIDER;
	}

	static <V, C> ProjectionCollector.Provider<V, C> simple(
			Function<List<V>, C> converter) {
		return new SimpleProjectionCollector.Provider<>( converter );
	}

	static <V> ProjectionCollector.Provider<V, V[]> array(
			Class<? super V> componentType) {
		return ArrayProjectionCollector.provider( componentType );
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionCollector.Provider<V, Set<V>> set() {
		return SetProjectionCollector.PROVIDER;
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionCollector.Provider<V, SortedSet<V>> sortedSet() {
		return SortedSetProjectionCollector.PROVIDER;
	}

	static <V> ProjectionCollector.Provider<V, SortedSet<V>> sortedSet(
			Comparator<? super V> comparator) {
		return SortedSetComparatorProjectionCollector.provider( comparator );
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> ProjectionCollector.Provider<V, Optional<V>> optional() {
		return OptionalProjectionCollector.PROVIDER;
	}
}
