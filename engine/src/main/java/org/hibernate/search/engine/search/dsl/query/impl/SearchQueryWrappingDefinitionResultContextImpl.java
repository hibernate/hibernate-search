/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.BuildingRootSearchPredicateDslContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.impl.QuerySearchPredicateDslContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateContributorAggregator;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryWrappingDefinitionResultContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;


public final class SearchQueryWrappingDefinitionResultContextImpl<T, C, Q>
		implements SearchQueryResultContext<Q>, SearchQueryWrappingDefinitionResultContext<Q> {

	private final SearchTargetContext<C> targetContext;

	private final SearchQueryBuilder<T, C> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	private final SearchPredicateContributorAggregator<C> searchPredicateContributorAggregator;

	public SearchQueryWrappingDefinitionResultContextImpl(SearchTargetContext<C> targetContext,
			SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
		this.searchPredicateContributorAggregator = new SearchPredicateContributorAggregator<>();
	}

	public SearchQueryWrappingDefinitionResultContextImpl(
			SearchQueryWrappingDefinitionResultContextImpl<T, C, ?> original,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this( original.targetContext, original.searchQueryBuilder, searchQueryWrapperFactory );
	}

	@Override
	public <R> SearchQueryResultContext<R> asWrappedQuery(Function<Q, R> wrapperFactory) {
		return new SearchQueryWrappingDefinitionResultContextImpl<>( this,
				searchQueryWrapperFactory.andThen( wrapperFactory ) );
	}

	@Override
	public SearchQueryContext<Q> predicate(SearchPredicate predicate) {
		SearchPredicateFactory<? super C> factory = targetContext.getSearchPredicateFactory();
		SearchPredicateContributor<? super C> contributor = factory.toContributor( predicate );
		searchPredicateContributorAggregator.add( contributor );
		return getNext();
	}

	@Override
	public SearchQueryContext<Q> predicate(Consumer<? super SearchPredicateContainerContext<SearchPredicate>> dslPredicateContributor) {
		SearchPredicateContributor<? super C> contributor = toContributor(
				targetContext.getSearchPredicateFactory(), dslPredicateContributor
		);
		searchPredicateContributorAggregator.add( contributor );
		return getNext();
	}

	@Override
	public SearchPredicateContainerContext<SearchQueryContext<Q>> predicate() {
		SearchPredicateDslContext<SearchQueryContext<Q>, C> dslContext = new QuerySearchPredicateDslContextImpl<>(
				searchPredicateContributorAggregator, this::getNext
		);
		return new SearchPredicateContainerContextImpl<>( targetContext.getSearchPredicateFactory(), dslContext );
	}

	private <PC> SearchPredicateContributor<PC> toContributor(SearchPredicateFactory<PC> factory,
			Consumer<? super SearchPredicateContainerContext<SearchPredicate>> dslPredicateContributor) {
		BuildingRootSearchPredicateDslContextImpl<PC> dslContext =
				new BuildingRootSearchPredicateDslContextImpl<>( factory );
		SearchPredicateContainerContext<SearchPredicate> containerContext =
				new SearchPredicateContainerContextImpl<>( factory, dslContext );
		dslPredicateContributor.accept( containerContext );
		return dslContext.getResultingContributor();
	}

	private SearchQueryContext<Q> getNext() {
		return new SearchQueryContextImpl<>(
				targetContext, searchQueryBuilder, searchQueryWrapperFactory,
				searchPredicateContributorAggregator
		);
	}

}
