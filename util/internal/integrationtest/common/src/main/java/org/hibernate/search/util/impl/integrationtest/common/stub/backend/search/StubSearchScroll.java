/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.Set;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

public class StubSearchScroll<T> implements SearchScroll<T> {

	private final StubBackendBehavior behavior;
	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<T> rootProjection;

	public StubSearchScroll(StubBackendBehavior behavior, Set<String> indexNames, StubSearchWork work,
			StubSearchProjectionContext projectionContext, LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection) {
		this.behavior = behavior;
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
	}

	@Override
	public void close() {
		behavior.executeCloseScrollWork( indexNames );
	}

	@Override
	public SearchScrollResult<T> next() {
		return behavior.executeNextScrollWork( indexNames, work, projectionContext, loadingContext, rootProjection );
	}
}
