/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * A delegating {@link SearchSortContainerContext}.
 * <p>
 * Mainly useful when implementing a {@link SearchSortContainerContextExtension}.
 */
public class DelegatingSearchSortContainerContextImpl<N> implements SearchSortContainerContext<N> {

	private final SearchSortContainerContext<N> delegate;

	public DelegatingSearchSortContainerContextImpl(SearchSortContainerContext<N> delegate) {
		this.delegate = delegate;
	}

	@Override
	public ScoreSortContext<N> byScore() {
		return delegate.byScore();
	}

	@Override
	public NonEmptySortContext<N> byIndexOrder() {
		return delegate.byIndexOrder();
	}

	@Override
	public FieldSortContext<N> byField(String absoluteFieldPath) {
		return delegate.byField( absoluteFieldPath );
	}

	@Override
	public NonEmptySortContext<N> by(SearchSort sort) {
		return delegate.by( sort );
	}

	@Override
	public <T> T withExtension(SearchSortContainerContextExtension<N, T> extension) {
		return delegate.withExtension( extension );
	}

	@Override
	public <T> NonEmptySortContext<N> withExtensionOptional(
			SearchSortContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor) {
		return delegate.withExtensionOptional( extension, clauseContributor );
	}

	@Override
	public <T> NonEmptySortContext<N> withExtensionOptional(
			SearchSortContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<SearchSortContainerContext<N>> fallbackClauseContributor) {
		return delegate.withExtensionOptional( extension, clauseContributor, fallbackClauseContributor );
	}

	protected SearchSortContainerContext<N> getDelegate() {
		return delegate;
	}
}
