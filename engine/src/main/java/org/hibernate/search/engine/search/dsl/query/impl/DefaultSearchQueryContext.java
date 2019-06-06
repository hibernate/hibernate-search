/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

final class DefaultSearchQueryContext<H, C>
		extends AbstractSearchQueryContext<
						DefaultSearchQueryContext<H, C>,
						H,
						SearchPredicateFactoryContext,
						SearchSortFactoryContext,
						C
				>
		implements SearchQueryResultContext<DefaultSearchQueryContext<H, C>, H, SearchPredicateFactoryContext>,
				SearchQueryContext<DefaultSearchQueryContext<H, C>, H, SearchSortFactoryContext> {

	DefaultSearchQueryContext(IndexScope<C> indexScope, SearchQueryBuilder<H, C> searchQueryBuilder) {
		super( indexScope, searchQueryBuilder );
	}

	@Override
	protected SearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		// We don't extend anything.
		return predicateFactoryContext;
	}

	@Override
	protected SearchSortFactoryContext extendSortContext(SearchSortFactoryContext sortFactoryContext) {
		// We don't extend anything.
		return sortFactoryContext;
	}

	@Override
	protected DefaultSearchQueryContext<H, C> thisAsS() {
		return this;
	}
}
