/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;

public final class StubMappingHints implements BackendMappingHints {
	public static final StubMappingHints INSTANCE = new StubMappingHints();

	private StubMappingHints() {
	}

	@Override
	public String noEntityProjectionAvailable() {
		return getClass().getName() + "#noEntityProjectionAvailable";
	}

	@Override
	public String missingDecimalScale() {
		return getClass().getName() + "#missingDecimalScale";
	}

	@Override
	public String missingVectorDimension() {
		return getClass().getName() + "#missingVectorDimension";
	}
}
