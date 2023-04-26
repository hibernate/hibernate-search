/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.spi;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.ExtendedSearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

public abstract class AbstractExtendedSearchQueryOptionsStep<
				S extends SearchQueryOptionsStep<S, H, LOS, SF, AF>,
				H,
				R extends SearchResult<H>,
				SCR extends SearchScroll<H>,
				LOS,
				PDF extends SearchPredicateFactory,
				SF extends SearchSortFactory,
				AF extends SearchAggregationFactory,
				SC extends SearchQueryIndexScope<?>
		>
		extends AbstractSearchQueryOptionsStep<S, H, LOS, PDF, SF, AF, SC> {

	public AbstractExtendedSearchQueryOptionsStep(SC scope,
			SearchQueryBuilder<H> searchQueryBuilder,
			SearchLoadingContextBuilder<?, LOS> loadingContextBuilder) {
		super( scope, searchQueryBuilder, loadingContextBuilder );
	}

	@Override
	public abstract ExtendedSearchQuery<H, R, SCR> toQuery();

	@Override
	public R fetchAll() {
		return toQuery().fetchAll();
	}

	@Override
	public R fetch(Integer limit) {
		return toQuery().fetch( limit );
	}

	@Override
	public R fetch(Integer offset, Integer limit) {
		return toQuery().fetch( offset, limit );
	}

	@Override
	public SCR scroll(int chunkSize) {
		return toQuery().scroll( chunkSize );
	}
}
