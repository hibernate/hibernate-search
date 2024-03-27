/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentCreateContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;

class PojoMassIndexerAgentCreateContextImpl implements PojoMassIndexerAgentCreateContext {
	private final PojoMassIndexingMappingContext mappingContext;
	private final String tenantIdentifier;

	PojoMassIndexerAgentCreateContextImpl(PojoMassIndexingMappingContext mappingContext, String tenantIdentifier) {
		this.mappingContext = mappingContext;
		this.tenantIdentifier = tenantIdentifier;
	}

	@Override
	public ScheduledExecutorService newScheduledExecutor(int threads, String threadNamePrefix) {
		return mappingContext.threadPoolProvider()
				.newScheduledExecutor( threads,
						PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + threadNamePrefix );
	}

	@Override
	public String tenantIdentifier() {
		return tenantIdentifier;
	}
}
