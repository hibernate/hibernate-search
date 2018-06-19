/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;

/**
 * A DSL context used when calling {@link SearchQueryContext#sort()} to build the sort
 * in a fluid way (in the same call chain as the query).
 */
public final class QuerySearchSortDslContextImpl<N, C>
		implements SearchSortDslContext<N, C> {

	private final C collector;
	private final N nextContext;

	private List<SearchSortContributor<? super C>> sortContributors = new ArrayList<>();

	public QuerySearchSortDslContextImpl(C collector, N nextContext) {
		this.collector = collector;
		this.nextContext = nextContext;
	}

	@Override
	public void addContributor(SearchSortContributor<? super C> child) {
		this.sortContributors.add( child );
	}

	@Override
	public N getNextContext() {
		sortContributors.forEach( c -> c.contribute( collector ) );
		return nextContext;
	}

}
