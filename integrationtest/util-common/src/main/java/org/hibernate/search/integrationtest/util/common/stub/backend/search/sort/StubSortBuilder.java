/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.search.sort;

import org.hibernate.search.integrationtest.util.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.engine.search.dsl.sort.SortOrder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

public class StubSortBuilder implements ScoreSortBuilder<StubQueryElementCollector>,
		FieldSortBuilder<StubQueryElementCollector> {

	@Override
	public void missingFirst() {
		// No-op
	}

	@Override
	public void missingLast() {
		// No-op
	}

	@Override
	public void missingAs(Object value) {
		// No-op
	}

	@Override
	public void order(SortOrder order) {
		// No-op
	}

	@Override
	public void contribute(StubQueryElementCollector collector) {
		collector.simulateCollectCall();
	}
}
