/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.dsl.impl;

import org.hibernate.search.backend.lucene.search.aggregation.dsl.LuceneSearchAggregationFactory;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchScroll;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryWhereStep;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.backend.lucene.search.sort.dsl.LuceneSearchSortFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractExtendedSearchQueryOptionsStep;

class LuceneSearchQueryOptionsStepImpl<H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
				LuceneSearchQueryOptionsStep<H, LOS>,
				H,
				LuceneSearchResult<H>,
				LuceneSearchScroll<H>,
				LOS,
				LuceneSearchPredicateFactory,
				LuceneSearchSortFactory,
				LuceneSearchAggregationFactory,
				LuceneSearchQueryIndexScope<?>>
		implements LuceneSearchQueryWhereStep<H, LOS>, LuceneSearchQueryOptionsStep<H, LOS> {

	private final LuceneSearchQueryBuilder<H> searchQueryBuilder;

	LuceneSearchQueryOptionsStepImpl(LuceneSearchQueryIndexScope<?> scope,
			LuceneSearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
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
	protected LuceneSearchPredicateFactory predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected LuceneSearchSortFactory sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected LuceneSearchAggregationFactory aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}

}
