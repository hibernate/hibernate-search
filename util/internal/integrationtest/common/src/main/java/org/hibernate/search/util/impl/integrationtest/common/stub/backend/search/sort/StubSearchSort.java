/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort;

import org.hibernate.search.engine.search.sort.SearchSort;

public class StubSearchSort implements SearchSort {

	public static StubSearchSort from(SearchSort sort) {
		return (StubSearchSort) sort;
	}

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}
}
