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
import org.hibernate.search.engine.search.dsl.predicate.impl.QuerySearchPredicateBuildingRootContextImpl;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateContainerContextImpl;
import org.hibernate.search.engine.search.dsl.query.SearchQueryWrappingDefinitionResultContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;


public final class SearchQueryWrappingDefinitionResultContextImpl<T, C, Q>
		implements SearchQueryResultContext<Q>, SearchQueryWrappingDefinitionResultContext<Q> {

	private final SearchTargetContext<C> targetContext;

	private final SearchQueryBuilder<T, C> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	public SearchQueryWrappingDefinitionResultContextImpl(SearchTargetContext<C> targetContext,
			SearchQueryBuilder<T, C> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
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
		factory.toContributor( predicate )
				.contribute( searchQueryBuilder.getQueryElementCollector() );
		return getNext();
	}

	@Override
	public SearchQueryContext<Q> predicate(Consumer<? super SearchPredicateContainerContext<SearchPredicate>> predicateContributor) {
		toContributor( targetContext.getSearchPredicateFactory(), predicateContributor )
				.contribute( searchQueryBuilder.getQueryElementCollector() );
		return getNext();
	}

	private <PC> SearchPredicateContributor<PC> toContributor(SearchPredicateFactory<PC> factory,
			Consumer<? super SearchPredicateContainerContext<SearchPredicate>> predicateContributor) {
		QuerySearchPredicateBuildingRootContextImpl<PC> dslContext =
				new QuerySearchPredicateBuildingRootContextImpl<>( factory );
		SearchPredicateContainerContext<SearchPredicate> containerContext =
				new SearchPredicateContainerContextImpl<>( factory, dslContext );
		predicateContributor.accept( containerContext );
		return dslContext;
	}

	private SearchQueryContext<Q> getNext() {
		return new SearchQueryContextImpl<>( searchQueryBuilder, searchQueryWrapperFactory );
	}

}
