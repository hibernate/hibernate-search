/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;

/**
 * A {@link ProjectionAccumulator} that can accumulate any number of values into a {@link java.util.List}.
 *
 * @param <F> The type of (unconverted) field values.
 * @param <V> The type of field values after the projection converter was applied.
 */
public final class ListProjectionAccumulator<F, V> implements ProjectionAccumulator<F, V, List<F>, List<V>> {

	@SuppressWarnings("rawtypes")
	private static final Provider PROVIDER = new Provider() {
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

	@SuppressWarnings("unchecked") // PROVIDER works for any V.
	public static <V> Provider<V, List<V>> provider() {
		return PROVIDER;
	}

	private ListProjectionAccumulator() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public List<F> createInitial() {
		return new ArrayList<>();
	}

	@Override
	public List<F> accumulate(List<F> accumulated, F value) {
		accumulated.add( value );
		return accumulated;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<V> finish(List<F> accumulated, ProjectionConverter<? super F, ? extends V> converter,
			FromDocumentValueConvertContext context) {
		// Hack to avoid instantiating another list: we convert a List<F> into a List<V> just by replacing its elements.
		// It works *only* because we know the actual underlying type of the list,
		// and we know it can work just as well with V as with F.
		ListIterator<F> iterator = accumulated.listIterator();
		while ( iterator.hasNext() ) {
			F fieldValue = iterator.next();
			V convertedValue = converter.fromDocumentValue( fieldValue, context );
			( (ListIterator) iterator ).set( convertedValue );
		}
		return (List<V>) (List) accumulated;
	}
}
