/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.spi.SearchQueryContextImplementor;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

final class DefaultSearchQueryContext<T, C>
		extends AbstractSearchQueryContext<
						DefaultSearchQueryContext<T, C>,
						T,
						SearchPredicateFactoryContext,
						SearchSortContainerContext,
						C
				>
		implements SearchQueryContextImplementor<
				DefaultSearchQueryContext<T, C>,
				T,
				SearchPredicateFactoryContext,
				SearchSortContainerContext
				> {

	DefaultSearchQueryContext(IndexSearchScope<C> indexSearchScope, SearchQueryBuilder<T, C> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
	}

	@Override
	protected SearchPredicateFactoryContext extendPredicateContext(
			SearchPredicateFactoryContext predicateFactoryContext) {
		// We don't extend anything.
		return predicateFactoryContext;
	}

	@Override
	protected SearchSortContainerContext extendSortContext(SearchSortContainerContext sortContainerContext) {
		// We don't extend anything.
		return sortContainerContext;
	}

	@Override
	protected DefaultSearchQueryContext<T, C> thisAsS() {
		return this;
	}
}
