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
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface PojoSearchTargetDelegate<E> {

	Set<Class<? extends E>> getTargetedIndexedTypes();

	<O, Q> SearchQueryResultContext<Q> queryAsLoadedObjects(
			ObjectLoader<PojoReference, O> objectLoader,
			Function<SearchQuery<O>, Q> searchQueryWrapperFactory);

	<T, Q> SearchQueryResultContext<Q> queryAsReferences(
			Function<PojoReference, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory);

	<O, T, Q> SearchQueryResultContext<Q> queryAsProjections(
			ObjectLoader<PojoReference, O> objectLoader,
			Function<List<?>, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext projection();

}
