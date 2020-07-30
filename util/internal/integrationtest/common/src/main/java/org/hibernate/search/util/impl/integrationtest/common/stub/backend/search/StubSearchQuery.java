/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

final class StubSearchQuery<H> extends AbstractSearchQuery<H, SearchResult<H>>
		implements SearchQuery<H> {

	private final StubBackend backend;
	private final Set<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<H> rootProjection;

	StubSearchQuery(StubBackend backend, Set<String> indexNames, StubSearchWork.Builder workBuilder,
			StubSearchProjectionContext projectionContext,
			LoadingContext<?, ?> loadingContext, StubSearchProjection<H> rootProjection) {
		this.backend = backend;
		this.indexNames = indexNames;
		this.workBuilder = workBuilder;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
	}

	@Override
	public String queryString() {
		return getClass().getName() + "@" + Integer.toHexString( hashCode() );
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public SearchResult<H> fetch(Integer offset, Integer limit) {
		workBuilder.limit( limit ).offset( offset );
		return backend.getBehavior().executeSearchWork(
				indexNames, workBuilder.build(), projectionContext, loadingContext, rootProjection
		);
	}

	@Override
	public long fetchTotalHitCount() {
		return backend.getBehavior().executeCountWork( indexNames );
	}

	@Override
	public SearchScroll<H> scroll(Integer pageSize) {
		return backend.getBehavior().executeScrollWork(
				indexNames, workBuilder.build(), pageSize, projectionContext, loadingContext, rootProjection
		);
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		workBuilder.failAfter( timeout, timeUnit );
	}
}
