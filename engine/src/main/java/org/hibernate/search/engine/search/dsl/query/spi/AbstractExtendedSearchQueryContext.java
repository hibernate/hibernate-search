/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.query.ExtendedSearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public abstract class AbstractExtendedSearchQueryContext<
		S extends SearchQueryContext<S, H, SC>,
		H,
		R extends SearchResult<H>,
		PC extends SearchPredicateFactoryContext,
		SC extends SearchSortContainerContext,
		C
		>
		extends AbstractSearchQueryContext<S, H, PC, SC, C> {

	public AbstractExtendedSearchQueryContext(IndexSearchScope<C> indexSearchScope,
			SearchQueryBuilder<H, C> searchQueryBuilder) {
		super( indexSearchScope, searchQueryBuilder );
	}

	@Override
	public abstract ExtendedSearchQuery<H, R> toQuery();

	@Override
	public R fetch() {
		return toQuery().fetch();
	}

	@Override
	public R fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public R fetch(Integer limit, Integer offset) {
		return toQuery().fetch( limit, offset );
	}

}
