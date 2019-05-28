/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query.impl;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateFactoryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultContext;
import org.hibernate.search.backend.lucene.search.dsl.sort.LuceneSearchSortContainerContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractExtendedSearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

class LuceneSearchQueryContextImpl<H>
		extends AbstractExtendedSearchQueryContext<
				LuceneSearchQueryContext<H>,
				H,
				LuceneSearchResult<H>,
				LuceneSearchPredicateFactoryContext,
				LuceneSearchSortContainerContext,
				LuceneSearchQueryElementCollector
		>
		implements LuceneSearchQueryResultContext<H>, LuceneSearchQueryContext<H> {

	private final LuceneSearchQueryBuilder<H> searchQueryBuilder;

	LuceneSearchQueryContextImpl(LuceneIndexScope indexSearchScope,
			LuceneSearchQueryBuilder<H> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
		this.searchQueryBuilder = searchQueryBuilder;
	}

	@Override
	public LuceneSearchQuery<H> toQuery() {
		return searchQueryBuilder.build();
	}

	@Override
	protected LuceneSearchQueryContextImpl<H> thisAsS() {
		return this;
	}

	@Override
	protected LuceneSearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		return predicateFactoryContext.extension( LuceneExtension.get() );
	}

	@Override
	protected LuceneSearchSortContainerContext extendSortContext(
			SearchSortContainerContext sortContainerContext) {
		return sortContainerContext.extension( LuceneExtension.get() );
	}
}
