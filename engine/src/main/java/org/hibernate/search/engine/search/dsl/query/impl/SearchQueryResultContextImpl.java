/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;


public final class SearchQueryResultContextImpl<T, C, Q>
		implements SearchQueryResultContext<Q> {

	private final SearchTargetContext<C> targetContext;
	private final SearchQueryBuilder<T, C> searchQueryBuilder;
	private final Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory;

	private final SearchQueryPredicateCollector<? super C, ?> searchPredicateCollector;

	public SearchQueryResultContextImpl(SearchTargetContext<C> targetContext,
			SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
		this.searchPredicateCollector = new SearchQueryPredicateCollector<>(
				targetContext.getSearchPredicateBuilderFactory()
		);
	}

	@Override
	public SearchQueryContext<Q> predicate(SearchPredicate predicate) {
		searchPredicateCollector.collect( predicate );
		return getNext();
	}

	@Override
	public SearchQueryContext<Q> predicate(Function<? super SearchPredicateFactoryContext, SearchPredicateTerminalContext> dslPredicateContributor) {
		searchPredicateCollector.collect( dslPredicateContributor );
		return getNext();
	}

	private SearchQueryContext<Q> getNext() {
		return new SearchQueryContextImpl<>(
				targetContext, searchQueryBuilder, searchQueryWrapperFactory,
				searchPredicateCollector
		);
	}

}
