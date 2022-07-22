/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractAgentClusterLink<R> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final FailureHandler failureHandler;
	protected final Clock clock;
	protected final Duration pollingInterval;
	protected final Duration pulseInterval;
	protected final Duration pulseExpiration;

	// Accessible for test purposes
	final AgentPersister agentPersister;

	public AbstractAgentClusterLink(AgentPersister agentPersister,
			FailureHandler failureHandler, Clock clock,
			Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration) {
		this.agentPersister = agentPersister;
		this.failureHandler = failureHandler;
		this.clock = clock;
		this.pollingInterval = pollingInterval;
		this.pulseInterval = pulseInterval;
		this.pulseExpiration = pulseExpiration;
	}

	public final R pulse(AgentRepository agentRepository) {
		Instant now = clock.instant();
		Instant newExpiration = now.plus( pulseExpiration );

		// In order to avoid transaction deadlocks with some RDBMS (yes, I mean SQL Server),
		// we make sure that *all* reads (listing all agents) happen before the write (creating/updating self).
		// I'm not entirely sure, but I think it goes like this:
		// if we read, then write, then read again, then the second read can trigger a deadlock,
		// with each transaction holding a write lock on some rows
		// while waiting for a read lock on the whole table.
		List<Agent> allAgentsInIdOrder = agentRepository.findAllOrderById();

		Agent preExistingSelf = agentPersister.extractSelf( allAgentsInIdOrder );
		Agent self;
		if ( preExistingSelf != null ) {
			self = preExistingSelf;
		}
		else {
			self = agentPersister.createSelf( agentRepository, allAgentsInIdOrder, newExpiration );
		}

		log.tracef( "Agent '%s': starting pulse at %s with self = %s, all agents = %s",
				selfReference(), now, self, allAgentsInIdOrder );

		// In order to avoid transaction deadlocks with some RDBMS (and this time I mean Oracle),
		// we make sure that if we need to delete expired agents,
		// we do it without updating the agent representing self in the same transaction
		// (creation is fine).
		// I'm not entirely sure, but I think it goes like this:
		// if one (already created) agent starts, updates itself,
		// then before its transaction is finished another agent does the same,
		// then the first agent tries to delete the second agent because it expired,
		// then the second agent tries to delete the first agent because it expired,
		// all in the same transaction,  well... here you have a deadlock.
		List<Agent> timedOutAgents = allAgentsInIdOrder.stream()
				.filter( a -> !a.equals( self ) && a.getExpiration().isBefore( now ) )
				.collect( Collectors.toList() );
		if ( !timedOutAgents.isEmpty() ) {
			log.removingTimedOutAgents( selfReference(), timedOutAgents );
			agentRepository.delete( timedOutAgents );
			log.infof( "Agent '%s': reassessing the new situation in the next pulse",
					selfReference(), now );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
		}

		// Delay expiration in each pulse
		self.setExpiration( newExpiration );

		return doPulse( agentRepository, now, allAgentsInIdOrder, self );
	}

	protected abstract R doPulse(AgentRepository agentRepository, Instant now,
			List<Agent> allAgentsInIdOrder, Agent self);

	/**
	 * Instructs the processor to commit the transaction, wait for the given delay, then pulse again.
	 * <p>
	 * Use with:
	 * <ul>
	 * <li>pollingInterval to apply a minimal delay before the next pulse, to avoid hitting the database continuously.
	 *     Useful when waiting for external changes.</li>
	 * <li>pulseInterval to apply a large delay before the next pulse.
	 *     Useful when suspended and waiting for a reason to resume.</li>
	 * </ul>
	 */
	protected abstract R instructCommitAndRetryPulseAfterDelay(Instant now, Duration delay);

	public final void leaveCluster(AgentRepository agentRepository) {
		agentPersister.leaveCluster( agentRepository );
	}

	protected AgentReference selfReference() {
		return agentPersister.selfReference();
	}

}
