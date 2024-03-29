/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
