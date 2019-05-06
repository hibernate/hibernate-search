/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.function.Function;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchScopeModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

public class StubSearchQueryBuilder<T> implements SearchQueryBuilder<T, StubQueryElementCollector> {

	private final StubBackend backend;
	private final StubSearchScopeModel scopeModel;
	private final StubSearchWork.Builder workBuilder;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final StubSearchProjection<T> rootProjection;

	public StubSearchQueryBuilder(StubBackend backend, StubSearchScopeModel scopeModel,
			StubSearchWork.ResultType resultType,
			FromDocumentFieldValueConvertContext convertContext,
			ProjectionHitMapper<?, ?> projectionHitMapper, StubSearchProjection<T> rootProjection) {
		this.backend = backend;
		this.scopeModel = scopeModel;
		this.workBuilder = StubSearchWork.builder( resultType );
		this.convertContext = convertContext;
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
	}

	@Override
	public StubQueryElementCollector getQueryElementCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public void addRoutingKey(String routingKey) {
		workBuilder.routingKey( routingKey );
	}

	@Override
	public <Q> Q build(Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		StubIndexSearchQuery<T> searchQuery = new StubIndexSearchQuery<>(
				backend, scopeModel.getIndexNames(), workBuilder, convertContext,
				projectionHitMapper, rootProjection
		);

		return searchQueryWrapperFactory.apply( searchQuery );
	}
}
