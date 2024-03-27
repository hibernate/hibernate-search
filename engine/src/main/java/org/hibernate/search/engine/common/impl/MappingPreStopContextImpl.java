/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common.impl;

import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class MappingPreStopContextImpl implements MappingPreStopContext {
	private final ContextualFailureCollector failureCollector;

	MappingPreStopContextImpl(ContextualFailureCollector failureCollector) {
		this.failureCollector = failureCollector;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return failureCollector;
	}

}
