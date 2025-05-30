/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

class LuceneSearchQueryOptionsStepImpl<SR, H, LOS>
		extends AbstractExtendedSearchQueryOptionsStep<
				SR,
				LuceneSearchQueryOptionsStep<SR, H, LOS>,
				H,
				LuceneSearchResult<H>,
				LuceneSearchScroll<H>,
				LOS,
				LuceneSearchPredicateFactory<SR>,
				LuceneSearchSortFactory<SR>,
				LuceneSearchAggregationFactory<SR>,
				LuceneSearchQueryIndexScope<SR, ?>>
		implements LuceneSearchQueryWhereStep<SR, H, LOS>, LuceneSearchQueryOptionsStep<SR, H, LOS> {

	private final LuceneSearchQueryBuilder<H> searchQueryBuilder;

	LuceneSearchQueryOptionsStepImpl(LuceneSearchQueryIndexScope<SR, ?> scope,
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
	protected LuceneSearchQueryOptionsStepImpl<SR, H, LOS> thisAsS() {
		return this;
	}

	@Override
	protected LuceneSearchPredicateFactory<SR> predicateFactory() {
		return scope.predicateFactory();
	}

	@Override
	protected LuceneSearchSortFactory<SR> sortFactory() {
		return scope.sortFactory();
	}

	@Override
	protected LuceneSearchAggregationFactory<SR> aggregationFactory() {
		return scope.aggregationFactory();
	}

	@Override
	protected SearchHighlighterFactory highlighterFactory() {
		return scope.highlighterFactory();
	}

}
