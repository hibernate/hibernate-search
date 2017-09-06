/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryFactory;
import org.hibernate.search.engine.search.dsl.SearchQueryContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class SearchQueryContextImpl<Q> implements SearchQueryContext<Q> {

	private final ElasticsearchSearchQueryFactory<Q> queryFactory;

	private final JsonObject rootQueryClause;

	public SearchQueryContextImpl(ElasticsearchSearchQueryFactory<Q> queryFactory, JsonObject rootQueryClause) {
		this.queryFactory = queryFactory;
		this.rootQueryClause = rootQueryClause;
	}

	@Override
	public Q build() {
		return queryFactory.create( rootQueryClause );
	}

}
