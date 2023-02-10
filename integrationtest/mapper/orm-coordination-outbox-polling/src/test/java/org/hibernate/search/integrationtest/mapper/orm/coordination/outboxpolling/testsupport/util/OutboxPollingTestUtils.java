/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.coordination.outboxpolling.testsupport.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.PersistenceRunner;

public class OutboxPollingTestUtils {

	private OutboxPollingTestUtils() {
	}

	// Wait for all agents to be registered and member of the same cluster.
	// Useful in tests checking indexing count for each agent,
	// because for those tests, starting with a partially-formed cluster could skew the numbers.
	public static void awaitAllAgentsRunningInOneCluster(PersistenceRunner<Session, Transaction> runner, int expectedAgentCount) {
		await( "Waiting for the formation of a cluster of " + expectedAgentCount + " agents" )
				.pollDelay( Duration.ZERO )
				.pollInterval( Duration.ofMillis( 5 ) )
				.atMost( Duration.ofSeconds( 5 ) )
				.untilAsserted( () -> {
					runner.runInTransaction( session -> {
						List<Agent> agents = session.createQuery( "select a from Agent a order by a.id", Agent.class )
								.list();
						assertThat( agents )
								.hasSize( expectedAgentCount )
								.allSatisfy( agent -> {
									assertThat( agent.getState() ).isEqualTo( AgentState.RUNNING );
									assertThat( agent.getTotalShardCount() ).isEqualTo( expectedAgentCount );
								} )
								.extracting( Agent::getAssignedShardIndex )
								.containsExactlyInAnyOrder( IntStream.range( 0, expectedAgentCount ).boxed().toArray( Integer[]::new ) );
					} );
				} );
	}
}
