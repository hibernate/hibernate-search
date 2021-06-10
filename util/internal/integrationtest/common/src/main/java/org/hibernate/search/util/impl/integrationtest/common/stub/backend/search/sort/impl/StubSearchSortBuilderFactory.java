/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl;

import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubQueryElementCollector;

public class StubSearchSortBuilderFactory
		implements SearchSortBuilderFactory<StubQueryElementCollector> {

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
	public SearchSort indexOrder() {
		return new StubSearchSort.Builder().build();
	}

	@Override
	public CompositeSortBuilder composite() {
		return new StubSearchSort.Builder();
	}
}
