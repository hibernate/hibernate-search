/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexerAgentStartContext;

class PojoMassIndexerAgentStartContextImpl implements PojoMassIndexerAgentStartContext {

	private final ScheduledExecutorService scheduledExecutorService;
	private final FailureHandler failureHandler;

	PojoMassIndexerAgentStartContextImpl(ScheduledExecutorService scheduledExecutorService,
			FailureHandler failureHandler) {
		this.scheduledExecutorService = scheduledExecutorService;
		this.failureHandler = failureHandler;
	}

	@Override
	public ScheduledExecutorService scheduledExecutor() {
		return scheduledExecutorService;
	}

	@Override
	public FailureHandler failureHandler() {
		return failureHandler;
	}
}
