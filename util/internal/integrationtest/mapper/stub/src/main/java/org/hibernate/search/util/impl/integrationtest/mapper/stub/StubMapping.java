/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.common.spi.SearchIntegration;

public interface StubMapping extends AutoCloseable, BackendMappingContext {

	@Override
	void close();

	SearchIntegration integration();

	StubSession session();

	StubSession session(Object tenantId);

	/**
	 * @return A fixture for this mapping, to easily set various mapping-related components.
	 */
	StubMappingFixture with();

}
