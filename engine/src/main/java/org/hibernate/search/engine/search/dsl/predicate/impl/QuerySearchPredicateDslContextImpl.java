/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;

/**
 * A DSL context used when calling {@link SearchQueryResultContext#predicate()} to build the predicate
 * in a fluid way (in the same call chain as the query).
 */
public final class QuerySearchPredicateDslContextImpl<N, C>
		implements SearchPredicateDslContext<N, C> {

	private final SearchPredicateContributorAggregator<C> aggregator;

	private final Supplier<N> nextContextSupplier;

	public QuerySearchPredicateDslContextImpl(SearchPredicateContributorAggregator<C> aggregator,
			Supplier<N> nextContextSupplier) {
		this.aggregator = aggregator;
		this.nextContextSupplier = nextContextSupplier;
	}

	@Override
	public void addContributor(SearchPredicateContributor<? super C> child) {
		aggregator.add( child );
	}

	@Override
	public N getNextContext() {
		return nextContextSupplier.get();
	}
}
