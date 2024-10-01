/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link java.util.List}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
@SuppressWarnings("deprecation")
final class ListProjectionAccumulator<E, V> extends ListBasedProjectionAccumulator<E, V, List<V>>
		implements ProjectionAccumulator<E, V, List<Object>, List<V>> {

	@SuppressWarnings("rawtypes")
	static final ProjectionAccumulator.Provider PROVIDER = new ProjectionAccumulator.Provider() {
		private final ListProjectionAccumulator instance = new ListProjectionAccumulator();

		@Override
		public ProjectionAccumulator get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	};

	private ListProjectionAccumulator() {
	}

	@Override
	public List<V> doFinish(List<V> accumulated) {
		return accumulated;
	}
}
