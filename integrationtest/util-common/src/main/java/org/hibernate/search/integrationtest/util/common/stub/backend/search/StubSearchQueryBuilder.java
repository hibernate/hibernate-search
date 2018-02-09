/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.search;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.integrationtest.util.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public class StubSearchQueryBuilder<T> implements SearchQueryBuilder<T, StubQueryElementCollector> {

	private final StubBackend backend;
	private final List<String> indexNames;
	private final StubSearchWork.Builder workBuilder;
	private final HitAggregator<?, List<T>> hitAggregator;

	public StubSearchQueryBuilder(StubBackend backend, List<String> indexNames, StubSearchWork.ResultType resultType,
			HitAggregator<?, List<T>> hitAggregator) {
		this.backend = backend;
		this.indexNames = indexNames;
		this.workBuilder = StubSearchWork.builder( resultType );
		this.hitAggregator = hitAggregator;
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
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		StubSearchQuery<T> searchQuery = new StubSearchQuery<>( backend, indexNames, workBuilder, hitAggregator );
		return searchQueryWrapperFactory.apply( searchQuery );
	}
}
