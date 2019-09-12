/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactory;
import org.hibernate.search.engine.search.dsl.query.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;

/**
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link SearchQueryHitTypeStep#asEntityReference()} reference queries},
 * @param <E> The type of loaded entities, i.e. the type of hits returned by
 * {@link SearchQueryHitTypeStep#asEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 * @param <C> The type of indexed type extended contexts; i.e. the type of elements in the set returned by
 * {@link #getIncludedIndexedTypes()}.
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 */
public interface PojoScopeDelegate<R, E, C> {

	Set<C> getIncludedIndexedTypes();

	SearchQueryHitTypeStep<?, R, E, SearchProjectionFactory<R, E>, ?> search(
			LoadingContextBuilder<R, E> loadingContextBuilder);

	SearchPredicateFactory predicate();

	SearchSortFactory sort();

	SearchProjectionFactory<R, E> projection();

	SearchAggregationFactory aggregation();

	PojoScopeWorkExecutor executor();

}
