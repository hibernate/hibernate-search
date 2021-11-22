/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.automaticindexing;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepositoryProvider;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.DefaultAgentRepository;

public class DisconnectionSimulatingAgentRepositoryProvider implements AgentRepositoryProvider {
	private volatile boolean preventPulse = true;

	public void setPreventPulse(boolean preventPulse) {
		this.preventPulse = preventPulse;
	}

	private final AgentRepositoryProvider delegate = new DefaultAgentRepository.Provider();

	@Override
	public AgentRepository create(Session session) {
		// We simulate the disconnection when starting a pulse, because that's more convenient for tests:
		// if the disconnection happened during event processing,
		// it could impact the total number of indexed entities
		// (some entities would be indexed twice)
		// and that would mess with our indexing count assertions.
		if ( !preventPulse ) {
			throw new RuntimeException( "Simulating a disconnection from the database" );
		}
		return delegate.create( session );
	}

}
