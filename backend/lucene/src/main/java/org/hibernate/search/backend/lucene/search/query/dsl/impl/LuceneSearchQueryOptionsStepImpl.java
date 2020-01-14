/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.dsl.impl;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryPredicateStep;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractExtendedSearchQueryOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

class LuceneSearchQueryOptionsStepImpl<H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
						LuceneSearchQueryOptionsStep<H, LOS>,
						H,
						LuceneSearchResult<H>,
						LOS,
						LuceneSearchPredicateFactory,
						LuceneSearchSortFactory,
						LuceneSearchAggregationFactory,
						LuceneSearchQueryElementCollector
				>
		implements LuceneSearchQueryPredicateStep<H, LOS>, LuceneSearchQueryOptionsStep<H, LOS> {

	private final LuceneSearchQueryBuilder<H> searchQueryBuilder;

	LuceneSearchQueryOptionsStepImpl(LuceneIndexScope indexSearchScope,
			LuceneSearchQueryBuilder<H> searchQueryBuilder,
			LoadingContextBuilder<?, ?, LOS> loadingContextBuilder) {
		super( indexSearchScope, searchQueryBuilder, loadingContextBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public LuceneSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected LuceneSearchQueryOptionsStepImpl<H, LOS> thisAsS() {
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
