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

/**
 * Provides access to built-in projection accumulators.
 */
public interface BuiltInProjectionAccumulators {

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, V> nullable() {
		return SingleValuedProjectionAccumulator.PROVIDER;
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, List<V>> list() {
		return ListProjectionAccumulator.PROVIDER;
	}

	static <V, C> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, C> simple(
			Function<List<V>, C> converter) {
		return new SimpleProjectionAccumulator.Provider<>( converter );
	}

	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, V[]> array(
			Class<? super V> componentType) {
		return ArrayProjectionAccumulator.provider( componentType );
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, Set<V>> set() {
		return SetProjectionAccumulator.PROVIDER;
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, SortedSet<V>> sortedSet() {
		return SortedSetProjectionAccumulator.PROVIDER;
	}

	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, SortedSet<V>> sortedSet(
			Comparator<? super V> comparator) {
		return SortedSetComparatorProjectionAccumulator.provider( comparator );
	}

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	static <V> org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider<V, Optional<V>> optional() {
		return OptionalProjectionAccumulator.PROVIDER;
	}
}
