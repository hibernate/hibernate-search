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
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractDelegatingSearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

public class LuceneSearchQueryContextImpl<T>
		extends AbstractDelegatingSearchQueryContext<
				LuceneSearchQueryContext<T>,
				T,
				LuceneSearchPredicateFactoryContext,
				LuceneSearchSortContainerContext
				>
		implements LuceneSearchQueryResultContext<T>, LuceneSearchQueryContext<T> {

	private final LuceneSearchQueryBuilder<T> builder;

	public LuceneSearchQueryContextImpl(SearchQueryContextImplementor<?, T, ?, ?> original,
			LuceneSearchQueryBuilder<T> builder) {
		super( original );
		this.builder = builder;
	}

	@Override
	public LuceneSearchQuery<T> toQuery() {
		return builder.build();
	}

	@Override
	protected LuceneSearchQueryContextImpl<T> thisAsS() {
		return this;
	}

	@Override
	protected LuceneSearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		return predicateFactoryContext.extension( LuceneExtension.get() );
	}

	@Override
	protected LuceneSearchSortContainerContext extendSortContext(SearchSortContainerContext sortContainerContext) {
		return sortContainerContext.extension( LuceneExtension.get() );
	}
}
