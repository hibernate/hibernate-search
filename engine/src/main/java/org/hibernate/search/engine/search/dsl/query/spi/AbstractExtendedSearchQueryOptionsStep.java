/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.spi;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.query.ExtendedSearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public abstract class AbstractExtendedSearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<S, H, SF>,
				H,
				R extends SearchResult<H>,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				C
		>
		extends AbstractSearchQueryOptionsStep<S, H, PDF, SF, C> {

	public AbstractExtendedSearchQueryOptionsStep(IndexScope<C> indexScope,
			SearchQueryBuilder<H, C> searchQueryBuilder) {
		super( indexScope, searchQueryBuilder );
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
