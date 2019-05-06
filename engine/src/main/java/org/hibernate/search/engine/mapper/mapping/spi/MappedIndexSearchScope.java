/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * @param <R> The type of references, i.e. the type of hits returned by
 * {@link #queryAsReference(SessionContextImplementor, LoadingContextBuilder, Function) reference queries},
 * or the type of objects returned for {@link SearchProjectionFactoryContext#reference() reference projections}.
 * @param <O> The type of loaded objects, i.e. the type of hits returned by
 * {@link #queryAsLoadedObject(SessionContextImplementor, LoadingContextBuilder, Function) loaded object queries}
 * when not using any hit transformer,
 * or the type of objects returned for {@link SearchProjectionFactoryContext#object() loaded object projections}.
 */
public interface MappedIndexSearchScope<R, O> {

	<T, Q> SearchQueryResultContext<?, Q, ?> queryAsLoadedObject(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, T> loadingContextBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory);

	<Q> SearchQueryResultContext<?, Q, ?> queryAsReference(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, ?> loadingContextBuilder,
			Function<IndexSearchQuery<R>, Q> searchQueryWrapperFactory);

	/*
	 * IMPLEMENTATION NOTE: we *must* only accept a loading context with the same R/O type parameters as this class,
	 * otherwise some casts in ObjectProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 * In particular, we cannot accept a LoadingContextBuilder<R, T> like we do in queryAsLoadedObject(...).
	 */
	<T, Q> SearchQueryResultContext<?, Q, ?> queryAsProjection(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<T> projection);

	/*
	 * IMPLEMENTATION NOTE: we *must* only accept a loading context with the same R/O type parameters as this class,
	 * otherwise some casts in ObjectProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 * In particular, we cannot accept a LoadingContextBuilder<R, T> like we do in queryAsLoadedObject(...).
	 */
	<Q> SearchQueryResultContext<?, Q, ?> queryAsProjections(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder,
			Function<IndexSearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	/*
	 * IMPLEMENTATION NOTE: we *must* return a factory with the same R/O type arguments as this class,
	 * otherwise some casts in ObjectProjectionContextImpl and ReferenceProjectionContextImpl
	 * will be wrong.
	 */
	SearchProjectionFactoryContext<R, O> projection();

}
