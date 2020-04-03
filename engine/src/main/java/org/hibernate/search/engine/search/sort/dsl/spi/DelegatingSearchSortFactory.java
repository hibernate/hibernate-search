/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.ExtendedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.sort.dsl.impl.DistanceSortOptionsStepImpl;
import org.hibernate.search.engine.search.sort.dsl.impl.FieldSortOptionsStepImpl;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A delegating {@link SearchSortFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchSortFactoryExtension}.
 */
public class DelegatingSearchSortFactory<PDF extends SearchPredicateFactory> implements ExtendedSearchSortFactory<PDF> {

	private final SearchSortFactory delegate;
	private final SearchSortDslContext<?, ?, ? extends PDF> dslContext;

	public DelegatingSearchSortFactory(SearchSortFactory delegate, SearchSortDslContext<?, ?, ? extends PDF> dslContext) {
		this.delegate = delegate;
		this.dslContext = dslContext;
	}

	@Override
	public ScoreSortOptionsStep<?> score() {
		return delegate.score();
	}

	@Override
	public SortThenStep indexOrder() {
		return delegate.indexOrder();
	}

	@Override
	public FieldSortOptionsStep<?, PDF> field(String absoluteFieldPath) {
		return new FieldSortOptionsStepImpl<>( dslContext, absoluteFieldPath );
	}

	@Override
	public DistanceSortOptionsStep<?, PDF> distance(String absoluteFieldPath, GeoPoint location) {
		return new DistanceSortOptionsStepImpl<>( dslContext, absoluteFieldPath, location );
	}

	@Override
	public CompositeSortComponentsStep<?> composite() {
		return delegate.composite();
	}

	@Override
	public SortThenStep composite(Consumer<? super CompositeSortComponentsStep<?>> elementContributor) {
		return delegate.composite( elementContributor );
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep extension() {
		return delegate.extension();
	}

	protected SearchSortFactory getDelegate() {
		return delegate;
	}
}
