/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.impl.StubSearchTargetModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.StubHitExtractor;

public class StubSearchQueryBuilder<T> implements SearchQueryBuilder<T, StubQueryElementCollector> {

	private final StubBackend backend;
	private final StubSearchTargetModel searchTargetModel;
	private final StubSearchWork.Builder workBuilder;
	private final StubHitExtractor<?, List<T>> hitExtractor;

	public StubSearchQueryBuilder(StubBackend backend, StubSearchTargetModel searchTargetModel,
			StubSearchWork.ResultType resultType, StubHitExtractor<?, List<T>> hitExtractor) {
		this.backend = backend;
		this.searchTargetModel = searchTargetModel;
		this.workBuilder = StubSearchWork.builder( resultType );
		this.hitExtractor = hitExtractor;
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
		StubSearchQuery<T> searchQuery = new StubSearchQuery<>(
				backend, searchTargetModel.getIndexNames(), workBuilder, hitExtractor
		);
		return searchQueryWrapperFactory.apply( searchQuery );
	}
}
