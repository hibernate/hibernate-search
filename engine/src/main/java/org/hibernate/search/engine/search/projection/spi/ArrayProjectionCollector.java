/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.lang.reflect.Array;
import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * A {@link ProjectionCollector} that can accumulate any number of values into a {@code V[]}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class ArrayProjectionCollector<E, V> extends ListBasedProjectionCollector<E, V, V[]> {

	static <U, R> Provider<U, R> provider(Class<? super U> elementType) {
		return new ArrayProvider<>( elementType );
	}

	private ArrayProjectionCollector(Class<? super V> elementType) {
		this.elementType = elementType;
	}

	private final Class<? super V> elementType;

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

	@SuppressWarnings("unchecked")
	private static class ArrayProvider<U, R> implements Provider<U, R> {
		private final ArrayProjectionCollector<?, U> instance;

		private ArrayProvider(Class<? super U> elementType) {
			instance = new ArrayProjectionCollector<>( elementType );
		}

		@Override
		public <T> ProjectionCollector<T, U, ?, R> get() {
			return (ProjectionCollector<T, U, ?, R>) instance;
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	}
}
