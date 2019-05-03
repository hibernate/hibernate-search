/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.spi;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.mapper.pojo.search.PojoReference;

/**
 * @param <E> A common supertype of the indexed types included in this scope.
 * @param <O> The type of loaded objects, i.e. the type of hits returned by
 * {@link #queryAsLoadedObject(ObjectLoader, Function) loaded object queries} when not using any hit transformer,
 * or the type of objects returned for {@link SearchProjectionFactoryContext#object() loaded object projections}.
 */
public interface PojoSearchScopeDelegate<E, O> {
	Set<Class<? extends E>> getIncludedIndexedTypes();

	<T, Q> SearchQueryResultContext<?, Q> queryAsLoadedObject(
			ObjectLoader<PojoReference, T> objectLoader,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory);

	<Q> SearchQueryResultContext<?, Q> queryAsReference(
			Function<IndexSearchQuery<PojoReference>, Q> searchQueryWrapperFactory);

	<T, Q> SearchQueryResultContext<?, Q> queryAsProjection(
			ObjectLoader<PojoReference, O> objectLoader,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<T> projection);

	<Q> SearchQueryResultContext<?, Q> queryAsProjections(
			ObjectLoader<PojoReference, O> objectLoader,
			Function<IndexSearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext<PojoReference, O> projection();

}
