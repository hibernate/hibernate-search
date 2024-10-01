/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link org.hibernate.search.engine.search.projection.ProjectionAccumulator} that can accumulate any number of values into a {@link SortedSet}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SortedSetProjectionAccumulator<E, V> extends ListBasedProjectionAccumulator<E, V, SortedSet<V>> {

	@SuppressWarnings("rawtypes")
	static final org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider PROVIDER =
			new org.hibernate.search.engine.search.projection.ProjectionAccumulator.Provider() {
				private final SortedSetProjectionAccumulator instance = new SortedSetProjectionAccumulator();

				@Override
				public org.hibernate.search.engine.search.projection.ProjectionAccumulator get() {
					return instance;
				}

				@Override
				public boolean isSingleValued() {
					return false;
				}
			};

	private SortedSetProjectionAccumulator() {
	}

	@Override
	public SortedSet<V> doFinish(List<V> accumulated) {
		return new TreeSet<>( accumulated );
	}
}
