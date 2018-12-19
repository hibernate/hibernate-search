/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.List;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

final class StubSearchQuery<T> implements SearchQuery<T> {

	private final StubBackend backend;
	private final List<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final StubSearchProjection<T> rootProjection;

	StubSearchQuery(StubBackend backend, List<String> indexNames, StubSearchWork.Builder workBuilder,
			FromDocumentFieldValueConvertContext convertContext,
			ProjectionHitMapper<?, ?> projectionHitMapper, StubSearchProjection<T> rootProjection) {
		this.backend = backend;
		this.indexNames = indexNames;
		this.workBuilder = workBuilder;
		this.convertContext = convertContext;
		this.projectionHitMapper = projectionHitMapper;
		this.rootProjection = rootProjection;
	}

	@Override
	public void setFirstResult(Long firstResultIndex) {
		workBuilder.firstResultIndex( firstResultIndex );
	}

	@Override
	public void setMaxResults(Long maxResultsCount) {
		workBuilder.maxResultsCount( maxResultsCount );
	}

	@Override
	public String getQueryString() {
		return getClass().getName() + "@" + Integer.toHexString( hashCode() );
	}

	@Override
	public SearchResult<T> execute() {
		return backend.getBehavior().executeSearchWork(
				indexNames, workBuilder.build(), convertContext, projectionHitMapper, rootProjection
		);
	}

	@Override
	public long executeCount() {
		return backend.getBehavior().executeCountWork( indexNames );
	}
}
