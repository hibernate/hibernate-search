/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl;

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
