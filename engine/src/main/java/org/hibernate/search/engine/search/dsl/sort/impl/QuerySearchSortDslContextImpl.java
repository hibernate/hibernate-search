/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;

/**
 * A DSL context used when calling {@link SearchQueryContext#sort()} to build the sort
 * in a fluid way (in the same call chain as the query).
 */
public final class QuerySearchSortDslContextImpl<N, C>
		implements SearchSortDslContext<N, C> {

	private final SearchSortContributorAggregator<C> searchSortContributorAggregator;
	private final N nextContext;

	public QuerySearchSortDslContextImpl(SearchSortContributorAggregator<C> searchSortContributorAggregator,
			N nextContext) {
		this.searchSortContributorAggregator = searchSortContributorAggregator;
		this.nextContext = nextContext;
	}

	@Override
	public void addContributor(SearchSortContributor<? super C> child) {
		this.searchSortContributorAggregator.add( child );
	}

	@Override
	public N getNextContext() {
		return nextContext;
	}

}
