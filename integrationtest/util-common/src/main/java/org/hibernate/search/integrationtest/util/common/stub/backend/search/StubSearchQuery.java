/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.search;

import java.util.List;

import org.hibernate.search.integrationtest.util.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.engine.search.query.spi.HitAggregator;

final class StubSearchQuery<T> implements SearchQuery<T> {

	private final StubBackend backend;
	private final List<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final HitAggregator<?, List<T>> hitAggregator;

	StubSearchQuery(StubBackend backend, List<String> indexNames, StubSearchWork.Builder workBuilder,
			HitAggregator<?, List<T>> hitAggregator) {
		this.backend = backend;
		this.indexNames = indexNames;
		this.workBuilder = workBuilder;
		this.hitAggregator = hitAggregator;
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
		return getClass().getName() + "@" + Integer.toHexString(hashCode());
	}

	@Override
	public SearchResult<T> execute() {
		return backend.getBehavior().executeSearchWork( indexNames, workBuilder.build(), hitAggregator );
	}

}
