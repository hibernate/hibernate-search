/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;

/**
 * A DSL context used when building a {@link SearchPredicate} object,
 * either when calling {@link IndexSearchTarget#predicate()} from a search target
 * or when calling {@link SearchQueryResultContext#predicate(Consumer)} to build the predicate using a lambda
 * (in which case the lambda may retrieve the resulting {@link SearchPredicate} object and cache it).
 */
public final class BuildingRootSearchPredicateDslContextImpl<CTX, C>
		implements SearchPredicateDslContext<SearchPredicate, CTX, C> {

	private final SearchPredicateFactory<CTX, C> factory;

	private final SearchPredicateContributorAggregator<CTX, C> aggregator = new SearchPredicateContributorAggregator<>();

	private SearchPredicate built;

	public BuildingRootSearchPredicateDslContextImpl(SearchPredicateFactory<CTX, C> factory) {
		this.factory = factory;
	}

	@Override
	public void addContributor(SearchPredicateContributor<CTX, ? super C> child) {
		aggregator.add( child );
	}

	@Override
	public SearchPredicate getNextContext() {
		if ( built == null ) {
			built = factory.toSearchPredicate( aggregator );
		}
		return built;
	}

	public SearchPredicateContributor<CTX, C> getResultingContributor() {
		if ( built != null ) {
			return factory.toContributor( built );
		}
		else {
			/*
			 * Optimization: we know the user will not be able to request a SearchSort anymore,
			 * so we don't need to build a SearchSort object in this case,
			 * we can just use the contributors directly.
			 */
			return aggregator;
		}
	}
}
