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
import org.hibernate.search.engine.search.dsl.sort.impl.BuildingRootSearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.QuerySearchSortDslContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortContainerContextImpl;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchSortContributorAggregator;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;


/**
 * @author Yoann Rodiere
 */
public final class SearchQueryContextImpl<T, Q, CTX, C> implements SearchQueryContext<Q> {

	private final SearchTargetContext<CTX, C> targetContext;
	private final SearchQueryBuilder<T, C> searchQueryBuilder;
	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;
	private final SearchPredicateContributor<CTX, ? super C> searchPredicateContributor;
	private final SearchSortContributorAggregator<C> searchSortContributorAggregator;

	public SearchQueryContextImpl(SearchTargetContext<CTX, C> targetContext, SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchPredicateContributor<CTX, ? super C> searchPredicateContributor) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
		this.searchPredicateContributor = searchPredicateContributor;
		this.searchSortContributorAggregator = new SearchSortContributorAggregator<>();
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
		SearchSortFactory<? super C> factory = targetContext.getSearchSortFactory();
		searchSortContributorAggregator.add( factory.toContributor( sort ) );
		return this;
	}

	@Override
	public SearchQueryContext<Q> sort(Consumer<? super SearchSortContainerContext<SearchSort>> dslSortContributor) {
		searchSortContributorAggregator.add(
				toContributor( targetContext.getSearchSortFactory(), dslSortContributor )
		);
		return this;
	}

	@Override
	public SearchSortContainerContext<SearchQueryContext<Q>> sort() {
		SearchSortDslContext<SearchQueryContext<Q>, C> dslContext =
				new QuerySearchSortDslContextImpl<>( searchSortContributorAggregator, this );
		return new SearchSortContainerContextImpl<>( targetContext.getSearchSortFactory(), dslContext );
	}

	private <C extends SC, SC> SearchSortContributor<? super C> toContributor(SearchSortFactory<SC> factory,
			Consumer<? super SearchSortContainerContext<SearchSort>> dslSortContributor) {
		BuildingRootSearchSortDslContextImpl<SC> dslContext =
				new BuildingRootSearchSortDslContextImpl<>( factory );
		SearchSortContainerContext<SearchSort> containerContext =
				new SearchSortContainerContextImpl<>( factory, dslContext );
		dslSortContributor.accept( containerContext );
		return dslContext.getResultingContributor();
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
		searchPredicateContributor.contribute( targetContext.getSearchPredicateFactory().createRootContext(), collector );
		searchSortContributorAggregator.contribute( collector );
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

}
