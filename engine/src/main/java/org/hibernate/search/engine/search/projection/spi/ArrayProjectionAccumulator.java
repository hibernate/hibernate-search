/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.lang.reflect.Array;
import java.util.List;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@code V[]}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class ArrayProjectionAccumulator<E, V> extends ListBasedProjectionAccumulator<E, V, V[]> {

	static <U, R> Provider<U, R> provider(Class<U> elementType) {
		return new ArrayProvider<>( elementType );
	}

	private ArrayProjectionAccumulator(Class<V> elementType) {
		this.elementType = elementType;
	}

	private final Class<V> elementType;

	@SuppressWarnings("unchecked")
	@Override
	public V[] doFinish(List<V> accumulated) {
		V[] array = (V[]) Array.newInstance( elementType, accumulated.size() );
		int i = 0;
		for ( V v : accumulated ) {
			array[i++] = v;
		}
		return array;
	}

	private static class ArrayProvider<U, R> implements Provider<U, R> {
		private final ArrayProjectionAccumulator<?, U> instance;

		private ArrayProvider(Class<U> elementType) {
			instance = new ArrayProjectionAccumulator<>( elementType );
		}

		@Override
		public <T> ProjectionAccumulator<T, U, ?, R> get() {
			return (ProjectionAccumulator<T, U, ?, R>) instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	}
}
