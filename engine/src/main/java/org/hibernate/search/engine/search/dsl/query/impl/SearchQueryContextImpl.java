/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;


public final class SearchQueryContextImpl<T, Q, C> implements SearchQueryContext<Q> {

	private final SearchQueryBuilder<T, C> searchQueryBuilder;
	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;
	private final SearchQueryPredicateCollector<? super C, ?> searchPredicateCollector;

	private final SearchQuerySortCollector<? super C, ?> searchSortCollector;

	public SearchQueryContextImpl(SearchTargetContext<C> targetContext, SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchQueryPredicateCollector<? super C, ?> searchPredicateCollector) {
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
		this.searchPredicateCollector = searchPredicateCollector;
		this.searchSortCollector = new SearchQuerySortCollector<>( targetContext.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchQueryContext<Q> routing(String routingKey) {
		searchQueryBuilder.addRoutingKey( routingKey );
		return this;
	}

	@Override
	public SearchQueryContext<Q> routing(Collection<String> routingKeys) {
		routingKeys.forEach( searchQueryBuilder::addRoutingKey );
		return this;
	}

	@Override
	public SearchQueryContext<Q> sort(SearchSort sort) {
		searchSortCollector.collect( sort );
		return this;
	}

	@Override
	public SearchQueryContext<Q> sort(Consumer<? super SearchSortContainerContext> dslSortContributor) {
		searchSortCollector.collect( dslSortContributor );
		return this;
	}

	@Override
	public Q build() {
		/*
		 * HSEARCH-3207: we must never call a contribution twice.
		 * Contributions may have side-effects, such as finishing the building of a boolean predicate by adding
		 * should clauses. Thus it's really not a good idea to execute a contribution twice,
		 * and delaying the contribution until the very end of the query building prevents that from ever happening.
		 *
		 * This means we must delay the contribution to some time when the user cannot use the DSL anymore
		 * (i.e. when this build() method is called),
		 * otherwise we'd need to execute the contribution upon some DSL method being called
		 * (an end() method for example), and this method could be called twice by the user.
		 */
		C collector = searchQueryBuilder.getQueryElementCollector();
		searchPredicateCollector.contribute( collector );
		searchSortCollector.contribute( collector );
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

}
