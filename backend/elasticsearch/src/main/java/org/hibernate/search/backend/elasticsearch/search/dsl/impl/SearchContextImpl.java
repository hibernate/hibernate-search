/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryFactory;
import org.hibernate.search.engine.search.dsl.SearchContext;
import org.hibernate.search.engine.search.dsl.SearchQueryContext;
import org.hibernate.search.engine.search.spi.SearchWrappingDefinitionContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class SearchContextImpl<Q> extends AbstractClauseContainerContext<SearchQueryContext<Q>>
		implements SearchContext<Q>, SearchWrappingDefinitionContext<Q> {

	private final ElasticsearchSearchQueryFactory<Q> queryFactory;

	private ClauseBuilder<JsonObject> rootQueryClauseBuilder;

	public SearchContextImpl(QueryTargetContext targetContext,
			ElasticsearchSearchQueryFactory<Q> queryFactory) {
		super( targetContext );
		this.queryFactory = queryFactory;
	}

	private SearchContextImpl(SearchContextImpl<?> original,
			ElasticsearchSearchQueryFactory<Q> newQueryFactory) {
		super( original );
		this.queryFactory = newQueryFactory;
	}

	@Override
	public <R> SearchContext<R> asWrappedQuery(Function<Q, R> wrapperFactory) {
		return new SearchContextImpl<R>( this,
				rootQuery -> wrapperFactory.apply( queryFactory.create( rootQuery ) ) );
	}

	@Override
	protected void add(ClauseBuilder<JsonObject> child) {
		this.rootQueryClauseBuilder = child;
	}

	@Override
	protected SearchQueryContext<Q> getNext() {
		JsonObject rootQueryClause = rootQueryClauseBuilder.build();
		return new SearchQueryContextImpl<>( queryFactory, rootQueryClause );
	}

}
