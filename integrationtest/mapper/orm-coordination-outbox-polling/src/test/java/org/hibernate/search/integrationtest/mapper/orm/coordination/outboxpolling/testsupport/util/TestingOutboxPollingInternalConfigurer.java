/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.impl.OutboxPollingInternalConfigurer;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TestingOutboxPollingInternalConfigurer implements OutboxPollingInternalConfigurer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private OutboxEventFilter outboxEventFilter;
	private OutboxAgentDisconnectionSimulator agentDisconnectionSimulator;

	public TestingOutboxPollingInternalConfigurer outboxEventFilter(OutboxEventFilter filter) {
		this.outboxEventFilter = filter;
		return this;
	}

	public TestingOutboxPollingInternalConfigurer agentDisconnectionSimulator(OutboxAgentDisconnectionSimulator agentDisconnectionSimulator) {
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
