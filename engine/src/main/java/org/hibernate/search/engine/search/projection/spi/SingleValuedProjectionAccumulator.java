/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link ProjectionAccumulator} that can accumulate up to one value, and will throw an exception beyond that.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class SingleValuedProjectionAccumulator<E, V> implements ProjectionAccumulator<E, V, Object, V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public E createInitial() {
		return null;
	}

	@Override
	public E accumulate(Object accumulated, E value) {
		if ( accumulated != null ) {
			throw log.unexpectedMultiValuedField( accumulated, value );
		}
		return value;
	}

	@Override
	public int size(Object accumulated) {
		return accumulated == null ? 0 : 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E get(Object accumulated, int index) {
		return (E) accumulated;
	}

	@Override
	public Object transform(Object accumulated, int index, V transformed) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( "Invalid index passed to " + this + ": " + index );
		}
		return transformed;
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
