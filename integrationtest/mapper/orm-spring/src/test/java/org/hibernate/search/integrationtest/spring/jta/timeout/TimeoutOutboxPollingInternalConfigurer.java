/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import java.util.List;
import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.DefaultOutboxEventFinder;
import org.hibernate.search.mapper.orm.outboxpolling.event.impl.OutboxEventFinderProvider;
import org.hibernate.search.mapper.orm.outboxpolling.impl.OutboxPollingInternalConfigurer;

public class TimeoutOutboxPollingInternalConfigurer implements OutboxPollingInternalConfigurer {
	@Override
	public OutboxEventFinderProvider wrapEventFinder(DefaultOutboxEventFinder.Provider delegate) {
		return delegate;
	}

	@Override
	public AgentRepositoryProvider wrapAgentRepository(AgentRepositoryProvider delegate) {
		return session -> {
			AgentRepository agentRepository = delegate.create( session );
			return new AgentRepository() {
				@Override
				public Agent find(UUID id) {
					return agentRepository.find( id );
				}

				@Override
				public List<Agent> findAllOrderById() {
					try {
						// this should provoke a timeout in TransactionTimeoutJtaAndSpringOutboxIT test
						Thread.sleep( 2000 );
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException( e );
					}
					return agentRepository.findAllOrderById();
				}

				@Override
				public void create(Agent agent) {
					agentRepository.create( agent );
				}

				@Override
				public void delete(List<Agent> agents) {
					agentRepository.delete( agents );
				}
			};
		};
	}
}
