/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryPredicateStep;
import org.hibernate.search.backend.elasticsearch.search.dsl.sort.ElasticsearchSearchSortFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchQuery;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractExtendedSearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;

class ElasticsearchSearchQueryOptionsStepImpl<H>
		extends AbstractExtendedSearchQueryOptionsStep<
		ElasticsearchSearchQueryOptionsStep<H>,
						H,
						ElasticsearchSearchResult<H>,
						ElasticsearchSearchPredicateFactory,
				ElasticsearchSearchSortFactory,
						ElasticsearchSearchQueryElementCollector
				>
		implements ElasticsearchSearchQueryPredicateStep<H>, ElasticsearchSearchQueryOptionsStep<H> {

	private final ElasticsearchSearchQueryBuilder<H> searchQueryBuilder;

	ElasticsearchSearchQueryOptionsStepImpl(ElasticsearchIndexScope indexSearchScope,
			ElasticsearchSearchQueryBuilder<H> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public ElasticsearchSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected ElasticsearchSearchQueryOptionsStepImpl<H> thisAsS() {
		return this;
	}

	@Override
	protected ElasticsearchSearchPredicateFactory extendPredicateFactory(
			SearchPredicateFactory predicateFactory) {
		return predicateFactory.extension( ElasticsearchExtension.get() );
	}

	@Override
	protected ElasticsearchSearchSortFactory extendSortFactory(
			SearchSortFactory sortFactory) {
		return sortFactory.extension( ElasticsearchExtension.get() );
	}
}
