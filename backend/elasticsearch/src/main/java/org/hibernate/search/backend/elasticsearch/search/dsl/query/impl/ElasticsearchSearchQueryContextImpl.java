/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortContainerContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexSearchScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractExtendedSearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

class ElasticsearchSearchQueryContextImpl<H>
		extends AbstractExtendedSearchQueryContext<
				ElasticsearchSearchQueryContext<H>,
				H,
				ElasticsearchSearchResult<H>,
				ElasticsearchSearchPredicateFactoryContext,
				ElasticsearchSearchSortContainerContext,
				ElasticsearchSearchQueryElementCollector
		>
		implements ElasticsearchSearchQueryResultContext<H>, ElasticsearchSearchQueryContext<H> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryContextImpl(ElasticsearchIndexSearchScope indexSearchScope,
			ElasticsearchSearchQueryBuilder<H> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public ElasticsearchSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected ElasticsearchSearchQueryContextImpl<H> thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchSearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		return predicateFactoryContext.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchSortContainerContext extendSortContext(
			SearchSortContainerContext sortContainerContext) {
		return sortContainerContext.extension( ElasticsearchExtension.get() );
	}
}
