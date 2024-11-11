/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * A {@link ProjectionCollector} that can accumulate any number of values into a {@link List},
 * and transforms that list into an arbitrary container on {@link #finish(List)}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 * @param <R> The type of the final result containing values of type {@code V}.
 */
@SuppressWarnings("deprecation")
abstract class ListBasedProjectionCollector<E, V, R>
		implements ProjectionCollector<E, V, List<Object>, R> {

	ListBasedProjectionCollector() {
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public final List<Object> createInitial() {
		return new ArrayList<>();
	}

	@Override
	public final List<Object> accumulate(List<Object> accumulated, E value) {
		accumulated.add( value );
		return accumulated;
	}

	@Override
	public final List<Object> accumulateAll(List<Object> accumulated, Collection<E> values) {
		accumulated.addAll( values );
		return accumulated;
	}

	@Override
	public final int size(List<Object> accumulated) {
		return accumulated.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final E get(List<Object> accumulated, int index) {
		return (E) accumulated.get( index );
	}

	@Override
	public final List<Object> transform(List<Object> accumulated, int index, V transformed) {
		accumulated.set( index, transformed );
		return accumulated;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final R finish(List<Object> accumulated) {
		// Hack to avoid instantiating another list: we convert a List<Object> into a List<U> just by replacing its elements.
		// It works *only* because we know the actual underlying type of the list,
		// and we know it can work just as well with U as with Object.
		return doFinish( (List<V>) (List) accumulated );
	}

	protected abstract R doFinish(List<V> accumulated);
}
