/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search;

public class StubQueryElementCollector {

	private static final StubQueryElementCollector INSTANCE = new StubQueryElementCollector();

	public static StubQueryElementCollector get() {
		return INSTANCE;
	}

	private StubQueryElementCollector() {
	}

	public void simulateCollectCall() {
		// No-op, just simulates a call on this object
	}

}
