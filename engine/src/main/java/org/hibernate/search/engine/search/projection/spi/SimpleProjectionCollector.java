/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.ProjectionCollector;

/**
 * A {@link ProjectionCollector} that can accumulate any number of values into a {@link List},
 * and transforms that list into an arbitrary container using a given {@link Function}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 * @param <R> The type of the final result containing values of type {@code V}.
 */
final class SimpleProjectionCollector<E, V, R> extends ListBasedProjectionCollector<E, V, R> {

	static final class Provider<V, R> implements ProjectionCollector.Provider<V, R> {
		private final Function<List<V>, R> finisher;

		Provider(Function<List<V>, R> finisher) {
			this.finisher = finisher;
		}

		@Override
		public <T> ProjectionCollector<T, V, ?, R> get() {
			return new SimpleProjectionCollector<>( finisher );
		}

		@Override
		public boolean isSingleValued() {
			return false;
		}
	}

	private final Function<List<V>, R> finisher;

	private SimpleProjectionCollector(Function<List<V>, R> finisher) {
		this.finisher = finisher;
	}

	@Override
	public R doFinish(List<V> accumulated) {
		return finisher.apply( accumulated );
	}
}
