/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.timeout.impl.StubTimeoutManager;

final class StubSearchQuery<H> extends AbstractSearchQuery<H, SearchResult<H>>
		implements SearchQuery<H> {

	private final StubBackend backend;
	private final Set<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final StubSearchProjectionContext projectionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final StubSearchProjection<H> rootProjection;

	StubSearchQuery(StubBackend backend, Set<String> indexNames, StubSearchWork.Builder workBuilder,
			StubSearchProjectionContext projectionContext,
			SearchLoadingContext<?> loadingContext, StubSearchProjection<H> rootProjection) {
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
		StubSearchWork work = workBuilder.build();
		StubTimeoutManager timeoutManager = new StubTimeoutManager(
				backend.timingSource(), work.getFailAfterTimeout(), work.getFailAfterTimeUnit() );
		timeoutManager.start();

		SearchResult<H> result = backend.getBehavior().executeSearchWork(
				indexNames, work, projectionContext, loadingContext, rootProjection, timeoutManager.hardDeadlineOrNull()
		);

		timeoutManager.stop();
		return result;
	}

	@Override
	public List<H> fetchHits(Integer offset, Integer limit) {
		return fetch( offset, limit ).hits();
	}

	@Override
	public long fetchTotalHitCount() {
		return backend.getBehavior().executeCountWork( indexNames );
	}

	@Override
	public SearchScroll<H> scroll(int chunkSize) {
		return backend.getBehavior().executeScrollWork(
				indexNames, workBuilder.build(), chunkSize, projectionContext, loadingContext, rootProjection,
				backend.timingSource()
		);
	}

	@Override
	public void failAfter(long timeout, TimeUnit timeUnit) {
		workBuilder.failAfter( timeout, timeUnit );
	}
}
