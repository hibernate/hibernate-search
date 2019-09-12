/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query.impl;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryPredicateStep;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractExtendedSearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;

class LuceneSearchQueryOptionsStepImpl<H>
		extends AbstractExtendedSearchQueryOptionsStep<
						LuceneSearchQueryOptionsStep<H>,
						H,
						LuceneSearchResult<H>,
						LuceneSearchPredicateFactory,
						LuceneSearchSortFactory,
						LuceneSearchAggregationFactory,
						LuceneSearchQueryElementCollector
				>
		implements LuceneSearchQueryPredicateStep<H>, LuceneSearchQueryOptionsStep<H> {

	private final LuceneSearchQueryBuilder<H> searchQueryBuilder;

	LuceneSearchQueryOptionsStepImpl(LuceneIndexScope indexSearchScope,
			LuceneSearchQueryBuilder<H> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public LuceneSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected LuceneSearchQueryOptionsStepImpl<H> thisAsS() {
		return this;
	}

	@Override
	protected LuceneSearchPredicateFactory extendPredicateFactory(
			SearchPredicateFactory predicateFactory) {
		return predicateFactory.extension( LuceneExtension.get() );
	}

	@Override
	protected LuceneSearchSortFactory extendSortFactory(
			SearchSortFactory sortFactory) {
		return sortFactory.extension( LuceneExtension.get() );
	}

	@Override
	protected LuceneSearchAggregationFactory extendAggregationFactory(SearchAggregationFactory aggregationFactory) {
		return aggregationFactory.extension( LuceneExtension.get() );
	}
}
