/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public ContextualFailureCollector getFailureCollector() {
		return failureCollector;
	}
}
