/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import java.util.Objects;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

public class StubSession implements BackendSessionContext {

	private final StubMapping mapping;
	private final Object tenantIdentifier;

	StubSession(StubMapping mapping, Object tenantIdentifier) {
		this.mapping = mapping;
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public BackendMappingContext mappingContext() {
		return mapping;
	}

	@Override
	public String tenantIdentifier() {
		return Objects.toString( tenantIdentifier, null );
	}
}
