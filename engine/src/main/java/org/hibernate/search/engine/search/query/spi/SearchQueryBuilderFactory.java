/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import java.util.List;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

/**
 * A factory for search query builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search queries.
 *
 * @param <C> The type of query element collector
 */
public interface SearchQueryBuilderFactory<C> {

	<E> SearchQueryBuilder<E, C> selectEntity(BackendSessionContext sessionContext,
			LoadingContextBuilder<?, E, ?> loadingContextBuilder);

	<R> SearchQueryBuilder<R, C> selectEntityReference(BackendSessionContext sessionContext,
			LoadingContextBuilder<R, ?, ?> loadingContextBuilder);

	<P> SearchQueryBuilder<P, C> select(BackendSessionContext sessionContext,
			LoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<P> projection);

	SearchQueryBuilder<List<?>, C> select(BackendSessionContext sessionContext,
			LoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<?>... projections);

}
