/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class MappingBuildContextImpl extends DelegatingBuildContext implements MappingBuildContext {

	private final ContextualFailureCollector failureCollector;

	MappingBuildContextImpl(RootBuildContext delegate, MappingKey<?, ?> mappingKey) {
		super( delegate );
		failureCollector = delegate.getFailureCollector().withContext( mappingKey );
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return failureCollector;
	}
}
