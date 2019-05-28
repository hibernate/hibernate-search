/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;

/**
 * @param <E> A common supertype of the indexed types included in this scope.
 * @param <E2> The type of loaded entities, i.e. the type of hits returned by
 * {@link SearchQueryResultDefinitionContext#asEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#entity() entity projections}.
 */
public interface PojoSearchScopeDelegate<E, E2> {
	Set<Class<? extends E>> getIncludedIndexedTypes();

	PojoReference toPojoReference(DocumentReference documentReference);

	SearchQueryResultDefinitionContext<?, PojoReference, E2, SearchProjectionFactoryContext<PojoReference, E2>, ?> search(
			LoadingContextBuilder<PojoReference, E2> loadingContextBuilder);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext<PojoReference, E2> projection();

}
