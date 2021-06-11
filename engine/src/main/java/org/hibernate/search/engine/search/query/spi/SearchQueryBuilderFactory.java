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
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;

/**
 * A factory for search query builders.
 * <p>
 * This is the main entry point for the engine
 * to ask the backend to build search queries.
 */
public interface SearchQueryBuilderFactory {

	<E> SearchQueryBuilder<E> selectEntity(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, E, ?> loadingContextBuilder);

	<R> SearchQueryBuilder<R> selectEntityReference(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<R, ?, ?> loadingContextBuilder);

	<P> SearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<P> projection);

	SearchQueryBuilder<List<?>> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<?>... projections);

}
