/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.spi.SearchQueryWrappingDefinitionResultContext;


/**
 * @author Yoann Rodiere
 */
public class SearchQueryWrappingDefinitionResultContextImpl<T, Q>
		implements SearchQueryResultContext<Q>, SearchQueryWrappingDefinitionResultContext<Q> {

	private final SearchTargetContext targetContext;

	private final ElasticsearchSearchQueryBuilder<T> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	public SearchQueryWrappingDefinitionResultContextImpl(SearchTargetContext targetContext,
			ElasticsearchSearchQueryBuilder<T> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.targetContext = targetContext;
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	public SearchQueryWrappingDefinitionResultContextImpl(SearchQueryWrappingDefinitionResultContextImpl<T, ?> original,
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
		ElasticsearchSearchPredicate.cast( predicate ).contribute( searchQueryBuilder::setRootQueryClause );
		return getNext();
	}

	@Override
	public SearchQueryContext<Q> predicate(Consumer<SearchPredicateContainerContext<SearchPredicate>> predicateContributor) {
		SingleSearchPredicateContainerContext context = new SingleSearchPredicateContainerContext( targetContext );
		predicateContributor.accept( context );
		context.contribute( searchQueryBuilder::setRootQueryClause );
		return getNext();
	}

	private SearchQueryContext<Q> getNext() {
		return new SearchQueryContextImpl<>( searchQueryBuilder, searchQueryWrapperFactory );
	}

}
