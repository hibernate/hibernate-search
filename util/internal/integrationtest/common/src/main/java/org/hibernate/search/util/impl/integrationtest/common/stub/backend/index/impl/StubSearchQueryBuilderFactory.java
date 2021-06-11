/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubCompositeListSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubEntitySearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubReferenceSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

class StubSearchQueryBuilderFactory implements SearchQueryBuilderFactory {
	private final StubBackend backend;
	private final StubSearchIndexScope scope;

	StubSearchQueryBuilderFactory(StubBackend backend, StubSearchIndexScope scope) {
		this.backend = backend;
		this.scope = scope;
	}

	@Override
	public <E> SearchQueryBuilder<E> selectEntity(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, E, ?> loadingContextBuilder) {
		return new StubSearchQueryBuilder<>(
				backend, scope, StubSearchWork.ResultType.OBJECTS,
				sessionContext,
				loadingContextBuilder,
				StubEntitySearchProjection.get()
		);
	}

	@Override
	public <R> SearchQueryBuilder<R> selectEntityReference(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<R, ?, ?> loadingContextBuilder) {
		return new StubSearchQueryBuilder<>(
				backend, scope, StubSearchWork.ResultType.REFERENCES,
				sessionContext,
				loadingContextBuilder,
				StubReferenceSearchProjection.get()
		);
	}

	@Override
	public <P> SearchQueryBuilder<P> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<P> projection) {
		return new StubSearchQueryBuilder<>(
				backend, scope, StubSearchWork.ResultType.PROJECTIONS,
				sessionContext,
				loadingContextBuilder,
				(StubSearchProjection<P>) projection
		);
	}

	@Override
	public SearchQueryBuilder<List<?>> select(BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder, SearchProjection<?>... projections) {
		return new StubSearchQueryBuilder<>(
				backend, scope, StubSearchWork.ResultType.PROJECTIONS,
				sessionContext,
				loadingContextBuilder,
				createRootProjection( projections )
		);
	}

	private StubSearchProjection<List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<StubSearchProjection<?>> children = new ArrayList<>( projections.length );

		for ( SearchProjection<?> projection : projections ) {
			children.add( (StubSearchProjection<?>) projection );
		}

		return new StubCompositeListSearchProjection<>( Function.identity(), children );
	}
}
