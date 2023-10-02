/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub;

public class StubTreeNodeMismatch {
	final Object expected;
	final Object actual;

	StubTreeNodeMismatch(Object expected, Object actual) {
		this.expected = expected;
		this.actual = actual;
	}
}
