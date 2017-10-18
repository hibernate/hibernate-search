/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.SearchContext;
import org.hibernate.search.engine.search.dsl.SearchQueryContext;
import org.hibernate.search.engine.search.spi.SearchWrappingDefinitionContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class SearchContextImpl<T, Q> extends AbstractClauseContainerContext<SearchQueryContext<Q>>
		implements SearchContext<Q>, SearchWrappingDefinitionContext<Q> {

	private final ElasticsearchSearchQueryBuilder<T> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	private ClauseBuilder<JsonObject> rootQueryClauseBuilder;

	public SearchContextImpl(QueryTargetContext targetContext,
			ElasticsearchSearchQueryBuilder<T> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		super( targetContext );
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	public SearchContextImpl(SearchContextImpl<T, ?> original,
			ElasticsearchSearchQueryBuilder<T> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		super( original );
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	@Override
	public <R> SearchContext<R> asWrappedQuery(Function<Q, R> wrapperFactory) {
		return new SearchContextImpl<>( this, searchQueryBuilder,
				searchQueryWrapperFactory.andThen( wrapperFactory ) );
	}

	@Override
	protected void add(ClauseBuilder<JsonObject> child) {
		this.rootQueryClauseBuilder = child;
	}

	@Override
	protected SearchQueryContext<Q> getNext() {
		JsonObject rootQueryClause = rootQueryClauseBuilder.build();
		searchQueryBuilder.setRootQueryClause( rootQueryClause );
		return new SearchQueryContextImpl<>( searchQueryBuilder, searchQueryWrapperFactory );
	}

}
