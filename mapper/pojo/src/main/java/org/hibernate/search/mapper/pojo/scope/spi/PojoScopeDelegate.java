/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;

/**
 * @param <E> The type of loaded entities, i.e. the type of hits returned by
 * {@link SearchQueryResultDefinitionContext#asEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 * @param <C> The type of indexed type extended contexts; i.e. the type of elements in the set returned by
 * {@link #getIncludedIndexedTypes()}.
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 */
public interface PojoScopeDelegate<E, C> {

	Set<C> getIncludedIndexedTypes();

	SearchQueryResultDefinitionContext<?, PojoReference, E, SearchProjectionFactoryContext<PojoReference, E>, ?> search(
			LoadingContextBuilder<PojoReference, E> loadingContextBuilder);

	SearchPredicateFactoryContext predicate();

	SearchSortFactoryContext sort();

	SearchProjectionFactoryContext<PojoReference, E> projection();

	PojoScopeWorkExecutor executor();

}
