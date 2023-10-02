/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.outboxpolling.testsupport.util;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TestingOutboxPollingInternalConfigurer implements OutboxPollingInternalConfigurer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
			log.debugf(
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
			log.debugf(
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
