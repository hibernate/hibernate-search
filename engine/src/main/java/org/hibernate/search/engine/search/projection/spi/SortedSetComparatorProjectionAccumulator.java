/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link org.hibernate.search.engine.search.projection.ProjectionAccumulator} that can accumulate any number of values into a {@link SortedSet}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SortedSetComparatorProjectionAccumulator<E, V> extends ListBasedProjectionAccumulator<E, V, SortedSet<V>> {

	static <U, R> Provider<U, R> provider(Comparator<? super U> comparator) {
		return new ComparatorBasedSortedSetProvider<>( comparator );
	}

	private SortedSetComparatorProjectionAccumulator(Comparator<? super V> comparator) {
		this.comparator = comparator;
	}

	private final Comparator<? super V> comparator;

	@Override
	public SortedSet<V> doFinish(List<V> accumulated) {
		TreeSet<V> set = new TreeSet<>( comparator );
		set.addAll( accumulated );
		return set;
	}

	@SuppressWarnings("unchecked")
	private static class ComparatorBasedSortedSetProvider<U, R> implements Provider<U, R> {
		private final SortedSetComparatorProjectionAccumulator<?, U> instance;

		private ComparatorBasedSortedSetProvider(Comparator<? super U> comparator) {
			instance = new SortedSetComparatorProjectionAccumulator<>( comparator );
		}

		@Override
		public <T> org.hibernate.search.engine.search.projection.ProjectionAccumulator<T, U, ?, R> get() {
			return (org.hibernate.search.engine.search.projection.ProjectionAccumulator<T, U, ?, R>) instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	}
}
