/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.SearchQueryContext;


/**
 * @author Yoann Rodiere
 */
class SearchQueryContextImpl<T, Q> implements SearchQueryContext<Q> {

	private final ElasticsearchSearchQueryBuilder<T> searchQueryBuilder;

	private final Function<SearchQuery<T>, Q> searchQueryWrapperFactory;

	public SearchQueryContextImpl(ElasticsearchSearchQueryBuilder<T> searchQueryBuilder,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		this.searchQueryBuilder = searchQueryBuilder;
		this.searchQueryWrapperFactory = searchQueryWrapperFactory;
	}

	@Override
	public Q build() {
		return searchQueryBuilder.build( searchQueryWrapperFactory );
	}

}
