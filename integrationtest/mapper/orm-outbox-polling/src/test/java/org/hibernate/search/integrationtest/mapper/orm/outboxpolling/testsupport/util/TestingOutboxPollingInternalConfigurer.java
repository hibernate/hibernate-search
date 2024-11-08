/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;


import static org.hibernate.search.util.impl.test.logging.impl.TestLog.TEST_LOGGER;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxPollingInternalConfigurer;

public class TestingOutboxPollingInternalConfigurer implements OutboxPollingInternalConfigurer {

	private OutboxEventFilter outboxEventFilter;
	private OutboxAgentDisconnectionSimulator agentDisconnectionSimulator;

	public TestingOutboxPollingInternalConfigurer outboxEventFilter(OutboxEventFilter filter) {
		this.outboxEventFilter = filter;
		return this;
	}

	public TestingOutboxPollingInternalConfigurer agentDisconnectionSimulator(
			OutboxAgentDisconnectionSimulator agentDisconnectionSimulator) {
		this.agentDisconnectionSimulator = agentDisconnectionSimulator;
		return this;
	}

	@Override
	public OutboxEventFinderProvider wrapEventFinder(DefaultOutboxEventFinder.Provider delegate) {
		if ( outboxEventFilter != null ) {
			TEST_LOGGER.debugf(
					"Outbox processing will use a filter for the outbox event finder: '%s'.",
					outboxEventFilter
			);
			return outboxEventFilter.wrap( delegate );
		}
		else {
			return delegate;
		}
	}

	@Override
	public AgentRepositoryProvider wrapAgentRepository(AgentRepositoryProvider delegate) {
		if ( agentDisconnectionSimulator != null ) {
			TEST_LOGGER.debugf(
					"Outbox processing will use a disconnection simulator for the agent repository provider '%s'.",
					agentDisconnectionSimulator
			);
			return agentDisconnectionSimulator.wrap( delegate );
		}
		else {
			return delegate;
		}
	}
}
