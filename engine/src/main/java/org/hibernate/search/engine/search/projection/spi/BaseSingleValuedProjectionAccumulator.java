/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A {@link org.hibernate.search.engine.search.projection.ProjectionAccumulator} that can accumulate up to one value, and will throw an exception beyond that.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
abstract class BaseSingleValuedProjectionAccumulator<E, V, R>
		implements org.hibernate.search.engine.search.projection.ProjectionAccumulator<E, V, Object, R> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected BaseSingleValuedProjectionAccumulator() {
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public final E createInitial() {
		return null;
	}

	@Override
	public final E accumulate(Object accumulated, E value) {
		if ( accumulated != null ) {
			throw log.unexpectedMultiValuedField( accumulated, value );
		}
		return value;
	}

	@Override
	public final int size(Object accumulated) {
		return accumulated == null ? 0 : 1;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final E get(Object accumulated, int index) {
		return (E) accumulated;
	}

	@Override
	public final Object transform(Object accumulated, int index, V transformed) {
		if ( index != 0 ) {
			throw new IndexOutOfBoundsException( "Invalid index passed to " + this + ": " + index );
		}
		return transformed;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object transformAll(Object accumulated, FromDocumentValueConverter<? super E, ? extends V> converter,
			FromDocumentValueConvertContext context) {
		return converter.fromDocumentValue( (E) accumulated, context );
	}
}
