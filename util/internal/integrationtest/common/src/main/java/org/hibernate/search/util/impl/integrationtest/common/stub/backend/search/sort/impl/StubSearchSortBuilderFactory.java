/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchSortBuilderFactory
		implements SearchSortBuilderFactory<StubQueryElementCollector> {

	private final StubSearchIndexScope scope;

	public StubSearchSortBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public void contribute(StubQueryElementCollector collector, SearchSort sort) {
		( (StubSearchSort) sort ).simulateBuild();
		collector.simulateCollectCall();
	}

	@Override
	public ScoreSortBuilder score() {
		return new StubSearchSort.Builder();
	}

	@Override
	public FieldSortBuilder field(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( SortTypeKeys.FIELD, scope );
	}

	@Override
	public SearchSort indexOrder() {
		return new StubSearchSort.Builder().build();
	}

	@Override
	public DistanceSortBuilder distance(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( SortTypeKeys.DISTANCE, scope );
	}

	@Override
	public CompositeSortBuilder composite() {
		return new StubSearchSort.Builder();
	}
}
