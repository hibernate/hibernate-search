/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl;

import static org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.DatabasePollingAgentAdditionalJaxbMappingProducer.ENTITY_NAME;

import java.util.List;

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
	public Agent find(Long id) {
		return session.find( Agent.class, id );
	}

	@Override
	public List<Agent> findAllOrderById() {
		return session.createQuery( "select a from " + ENTITY_NAME + " a order by id", Agent.class ).list();
	}

	@Override
	public void create(Agent agent) {
		session.persist( agent );
	}

	@Override
	public void delete(List<Agent> agents) {
		for ( Agent agent : agents ) {
			session.delete( agent );
		}
	}
}
