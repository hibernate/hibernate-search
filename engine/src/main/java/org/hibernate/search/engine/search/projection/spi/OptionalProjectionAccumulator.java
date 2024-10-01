/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.Optional;

/**
 * A {@link org.hibernate.search.engine.search.projection.ProjectionAccumulator} that can accumulate up to one value, and will throw an exception beyond that.
 * The value is wrapped in an {@link Optional}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class OptionalProjectionAccumulator<E, V> extends BaseSingleValuedProjectionAccumulator<E, V, Optional<V>> {

	@SuppressWarnings("rawtypes")
	static final Provider PROVIDER = new Provider() {
		private final OptionalProjectionAccumulator instance = new OptionalProjectionAccumulator();

		@Override
		public org.hibernate.search.engine.search.projection.ProjectionAccumulator get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return true;
		}
	};

	private OptionalProjectionAccumulator() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<V> finish(Object accumulated) {
		return Optional.ofNullable( (V) accumulated );
	}
}
