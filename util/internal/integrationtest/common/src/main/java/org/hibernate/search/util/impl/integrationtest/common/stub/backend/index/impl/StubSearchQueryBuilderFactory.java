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

import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchScopeModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubCompositeListSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubEntitySearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubReferenceSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

class StubSearchQueryBuilderFactory implements SearchQueryBuilderFactory<StubQueryElementCollector> {
	private final StubBackend backend;
	private final StubSearchScopeModel scopeModel;

	StubSearchQueryBuilderFactory(StubBackend backend, StubSearchScopeModel scopeModel) {
		this.backend = backend;
		this.scopeModel = scopeModel;
	}

	@Override
	public <E> SearchQueryBuilder<E, StubQueryElementCollector> asEntity(SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, E> loadingContextBuilder) {
		return new StubSearchQueryBuilder<>(
				backend, scopeModel, StubSearchWork.ResultType.OBJECTS,
				new FromDocumentFieldValueConvertContextImpl( sessionContext ),
				loadingContextBuilder,
				StubEntitySearchProjection.get()
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, StubQueryElementCollector> asReference(SessionContextImplementor sessionContext,
			LoadingContextBuilder<T, ?> loadingContextBuilder) {
		return new StubSearchQueryBuilder<>(
				backend, scopeModel, StubSearchWork.ResultType.REFERENCES,
				new FromDocumentFieldValueConvertContextImpl( sessionContext ),
				loadingContextBuilder,
				StubReferenceSearchProjection.get()
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, StubQueryElementCollector> asProjection(SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder, SearchProjection<T> projection) {
		return new StubSearchQueryBuilder<>(
				backend, scopeModel, StubSearchWork.ResultType.PROJECTIONS,
				new FromDocumentFieldValueConvertContextImpl( sessionContext ),
				loadingContextBuilder,
				(StubSearchProjection<T>) projection
		);
	}

	@Override
	public SearchQueryBuilder<List<?>, StubQueryElementCollector> asProjections(SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder, SearchProjection<?>... projections) {
		return new StubSearchQueryBuilder<>(
				backend, scopeModel, StubSearchWork.ResultType.PROJECTIONS,
				new FromDocumentFieldValueConvertContextImpl( sessionContext ),
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
