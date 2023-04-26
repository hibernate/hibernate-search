/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

import java.util.Set;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.timeout.impl.StubTimeoutManager;

public class StubSearchScroll<T> implements SearchScroll<T> {

	private final StubBackendBehavior behavior;
	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final StubSearchProjectionContext projectionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final StubTimeoutManager timeoutManager;

	public StubSearchScroll(StubBackendBehavior behavior, Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection, TimingSource timingSource) {
		this.behavior = behavior;
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.timeoutManager = new StubTimeoutManager( timingSource, work.getFailAfterTimeout(),
				work.getFailAfterTimeUnit() );
	}

	@Override
	public void close() {
		behavior.executeCloseScrollWork( indexNames );
	}

	@Override
	public SearchScrollResult<T> next() {
		timeoutManager.start();

		SearchScrollResult<T> result = behavior.executeNextScrollWork( indexNames, work, projectionContext,
				loadingContext, rootProjection, timeoutManager.hardDeadlineOrNull()
		);

		timeoutManager.stop();
		return result;
	}
}
