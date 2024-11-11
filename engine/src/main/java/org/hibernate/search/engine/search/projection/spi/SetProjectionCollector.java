/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * A {@link ProjectionCollector} that can accumulate any number of values into a {@link Set}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SetProjectionCollector<E, V> extends ListBasedProjectionCollector<E, V, Set<V>> {

	@SuppressWarnings("rawtypes")
	static final Provider PROVIDER = new Provider() {
		private final SetProjectionCollector instance = new SetProjectionCollector();

		@Override
		public ProjectionCollector get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	};

	private SetProjectionCollector() {
	}

	@Override
	public Set<V> doFinish(List<V> accumulated) {
		return new HashSet<>( accumulated );
	}
}
