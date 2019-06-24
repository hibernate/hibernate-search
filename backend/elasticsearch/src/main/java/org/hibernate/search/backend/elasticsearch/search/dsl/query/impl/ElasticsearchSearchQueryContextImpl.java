/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractExtendedSearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;

class ElasticsearchSearchQueryContextImpl<H>
		extends AbstractExtendedSearchQueryContext<
				ElasticsearchSearchQueryContext<H>,
				H,
				ElasticsearchSearchResult<H>,
				ElasticsearchSearchPredicateFactory,
				ElasticsearchSearchSortFactoryContext,
				ElasticsearchSearchQueryElementCollector
		>
		implements ElasticsearchSearchQueryResultContext<H>, ElasticsearchSearchQueryContext<H> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryContextImpl(ElasticsearchIndexScope indexSearchScope,
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
	protected ElasticsearchSearchPredicateFactory extendPredicateFactory(
			SearchPredicateFactory predicateFactory) {
		return predicateFactory.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchSortFactoryContext extendSortContext(
			SearchSortFactoryContext sortFactoryContext) {
		return sortFactoryContext.extension( ElasticsearchExtension.get() );
	}
}
