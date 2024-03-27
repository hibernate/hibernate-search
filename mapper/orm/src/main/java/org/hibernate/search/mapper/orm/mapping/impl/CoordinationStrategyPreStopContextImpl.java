/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.engine.mapper.mapping.spi.MappingPreStopContext;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.mapper.orm.coordination.common.spi.CoordinationStrategyPreStopContext;

public class CoordinationStrategyPreStopContextImpl implements CoordinationStrategyPreStopContext {
	private final MappingPreStopContext delegate;

	public CoordinationStrategyPreStopContextImpl(MappingPreStopContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public ContextualFailureCollector failureCollector() {
		return delegate.failureCollector();
	}
}
