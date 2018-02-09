/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.search.sort;

import org.hibernate.search.integrationtest.util.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.sort.spi.SearchSortContributor;

class StubSearchSort implements SearchSort, SearchSortContributor<StubQueryElementCollector> {
	@Override
	public void contribute(StubQueryElementCollector collector) {
		collector.simulateCollectCall();
	}
}
