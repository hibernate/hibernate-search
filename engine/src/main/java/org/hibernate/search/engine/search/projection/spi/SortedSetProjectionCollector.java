/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * A {@link ProjectionCollector} that can accumulate any number of values into a {@link SortedSet}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SortedSetProjectionCollector<E, V> extends ListBasedProjectionCollector<E, V, SortedSet<V>> {

	@SuppressWarnings("rawtypes")
	static final ProjectionCollector.Provider PROVIDER =
			new ProjectionCollector.Provider() {
				private final SortedSetProjectionCollector instance = new SortedSetProjectionCollector();

				@Override
				public ProjectionCollector get() {
					return instance;
				}

				@Override
				public boolean isSingleValued() {
					return false;
				}
			};

	private SortedSetProjectionCollector() {
	}

	@Override
	public SortedSet<V> doFinish(List<V> accumulated) {
		return new TreeSet<>( accumulated );
	}
}
