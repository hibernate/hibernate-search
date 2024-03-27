/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.List;
import java.util.UUID;

import org.hibernate.Session;

public class DefaultAgentRepository implements AgentRepository {
	public static final class Provider implements AgentRepositoryProvider {
		@Override
		public AgentRepository create(Session session) {
			return new DefaultAgentRepository( session );
		}
	}

	private final Session session;

	private DefaultAgentRepository(Session session) {
		this.session = session;
	}

	@Override
	public Agent find(UUID id) {
		return session.find( Agent.class, id );
	}

	@Override
	public List<Agent> findAllOrderById() {
		return session
				.createQuery( "select a from " + OutboxPollingAgentAdditionalJaxbMappingProducer.ENTITY_NAME + " a order by id",
						Agent.class )
				.list();
	}

	@Override
	public void create(Agent agent) {
		session.persist( agent );
	}

	@Override
	public void delete(List<Agent> agents) {
		for ( Agent agent : agents ) {
			session.remove( agent );
		}
	}
}
