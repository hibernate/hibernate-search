/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A base implementation of {@link ProjectionAccumulator} for multi-valued projections that can accumulate any number of values into a {@link List}.
 * <p>
 * Accumulators extending this abstract one can decide on the final collection returned by the accumulator.
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 * @param <R> The type of the final result containing values of type {@code V}.
 */
abstract class AbstractListBasedProjectionAccumulator<E, V, R> implements ProjectionAccumulator<E, V, List<Object>, R> {


	@Override
	public String toString() {
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
}
