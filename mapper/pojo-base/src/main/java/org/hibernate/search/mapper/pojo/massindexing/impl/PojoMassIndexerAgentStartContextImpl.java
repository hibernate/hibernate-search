/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentStartContext;

class PojoMassIndexerAgentStartContextImpl implements PojoMassIndexerAgentStartContext {

	private final ThreadPoolProvider threadPoolProvider;
	private final FailureHandler failureHandler;

	private ScheduledExecutorService scheduledExecutorService;

	PojoMassIndexerAgentStartContextImpl(ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler) {
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
	}

	@Override
	public ScheduledExecutorService scheduledExecutor() {
		if ( this.scheduledExecutorService == null ) {
			this.scheduledExecutorService = threadPoolProvider.newScheduledExecutor(
					1,
					PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + "Mass indexer agent"
			);
		}
		return scheduledExecutorService;
	}

	@Override
	public FailureHandler failureHandler() {
		return failureHandler;
	}

	public void clear() {
		if ( scheduledExecutorService != null ) {
			scheduledExecutorService.shutdownNow();
		}
	}
}
