/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link Set}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SetProjectionAccumulator<E, V> extends ListBasedProjectionAccumulator<E, V, Set<V>> {

	@SuppressWarnings("rawtypes")
	static final Provider PROVIDER = new Provider() {
		private final SetProjectionAccumulator instance = new SetProjectionAccumulator();

		@Override
		public org.hibernate.search.engine.search.projection.ProjectionAccumulator get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	};

	private SetProjectionAccumulator() {
	}

	@Override
	public Set<V> doFinish(List<V> accumulated) {
		return new HashSet<>( accumulated );
	}
}
