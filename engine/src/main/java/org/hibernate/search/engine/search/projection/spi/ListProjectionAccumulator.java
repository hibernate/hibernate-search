/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link java.util.List}.
 *
 * @param <E> The type of extracted values to accumulate before being transformed.
 * @param <V> The type of values to accumulate obtained by transforming extracted values ({@code E}).
 */
final class ListProjectionAccumulator<E, V> implements ProjectionAccumulator<E, V, List<Object>, List<V>> {

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
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public List<Object> createInitial() {
		return new ArrayList<>();
	}

	@Override
	public List<Object> accumulate(List<Object> accumulated, E value) {
		accumulated.add( value );
		return accumulated;
	}

	@Override
	public List<Object> accumulateAll(List<Object> accumulated, Collection<E> values) {
		accumulated.addAll( values );
		return accumulated;
	}

	@Override
	public int size(List<Object> accumulated) {
		return accumulated.size();
	}

	@Override
	@SuppressWarnings("unchecked")
	public E get(List<Object> accumulated, int index) {
		return (E) accumulated.get( index );
	}

	@Override
	public List<Object> transform(List<Object> accumulated, int index, V transformed) {
		accumulated.set( index, transformed );
		return accumulated;
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
