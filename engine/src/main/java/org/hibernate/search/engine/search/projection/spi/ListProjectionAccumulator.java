/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;

import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReference;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link java.util.List}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 *
 * @deprecated Use {@link ListBasedProjectionAccumulator#provider(MultiProjectionTypeReference)} with {@link MultiProjectionTypeReference#list()} instead.
 */
@Deprecated(since = "8.0")
final class ListProjectionAccumulator<E, V> extends AbstractListBasedProjectionAccumulator<E, V, List<V>> {

	@SuppressWarnings("rawtypes")
	static final Provider PROVIDER = new Provider() {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<V> finish(List<Object> accumulated) {
		// Hack to avoid instantiating another list: we convert a List<Object> into a List<U> just by replacing its elements.
		// It works *only* because we know the actual underlying type of the list,
		// and we know it can work just as well with U as with Object.
		return (List<V>) (List) accumulated;
	}
}
