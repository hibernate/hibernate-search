/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

class StubScoreProjection extends AbstractStubPassThroughProjection<Float> {
	static final StubScoreProjection INSTANCE = new StubScoreProjection();

	@Override
	protected String typeName() {
		return "score";
	}
}
