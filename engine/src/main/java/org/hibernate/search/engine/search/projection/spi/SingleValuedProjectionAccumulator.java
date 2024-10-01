/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

/**
 * A {@link ProjectionAccumulator} that can accumulate up to one value, and will throw an exception beyond that.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
@SuppressWarnings("deprecation")
final class SingleValuedProjectionAccumulator<E, V> extends BaseSingleValuedProjectionAccumulator<E, V, V>
		implements ProjectionAccumulator<E, V, Object, V> {

	@SuppressWarnings("rawtypes")
	static final ProjectionAccumulator.Provider PROVIDER = new ProjectionAccumulator.Provider() {
		private final SingleValuedProjectionAccumulator instance = new SingleValuedProjectionAccumulator();

		@Override
		public ProjectionAccumulator get() {
			return instance;
		}

		@Override
		public boolean isSingleValued() {
			return true;
		}
	};

	private SingleValuedProjectionAccumulator() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object transformAll(Object accumulated, ProjectionConverter<? super E, ? extends V> converter,
			FromDocumentValueConvertContext context) {
		return converter.fromDocumentValue( (E) accumulated, context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public V finish(Object accumulated) {
		return (V) accumulated;
	}
}
