/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

abstract class AbstractAgentClusterLink<R> implements ToStringTreeAppendable {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final FailureHandler failureHandler;
	protected final Clock clock;
	protected final Duration pollingInterval;
	protected final Duration pulseInterval;
	protected final Duration pulseExpiration;
	private final AgentPersister agentPersister;


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

	@Override
	public String toString() {
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "agentPersister", agentPersister )
				.attribute( "pollingInterval", pollingInterval )
				.attribute( "pulseInterval", pulseInterval )
				.attribute( "pulseExpiration", pulseExpiration );
	}

	public final R pulse(AgentClusterLinkContext context) {
		Agent self = ensureRegistered( context );

		// In order to avoid transaction deadlocks with some RDBMS (yes, I mean SQL Server),
		// we make sure that reads listing other agents always happen in a different transaction
		// than writes.
		List<Agent> allAgentsInIdOrder = context.agentRepository().findAllOrderById();

		Instant now = clock.instant();
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
		Instant expirationLimit = now;
		List<Agent> timedOutAgents = allAgentsInIdOrder.stream()
				.filter( Predicate.isEqual( self ).negate() ) // Ignore self: if expired, we'll correct that.
				.filter( a -> a.getExpiration().isBefore( expirationLimit ) )
				.collect( Collectors.toList() );
		if ( !timedOutAgents.isEmpty() ) {
			log.removingTimedOutAgents( selfReference(), timedOutAgents );
			context.agentRepository().delete( timedOutAgents );
			log.infof( "Agent '%s': reassessing the new situation in the next pulse", selfReference() );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
		}

		// Determine what needs to be done
		WriteAction<R> pulseResult = doPulse( allAgentsInIdOrder, self );

		// Write actions are always executed in a new transaction,
		// so that the risk of deadlocks (see above) is minimal,
		// and transactions are shorter (for lower transaction contention,
		// important on CockroachDB in particular:
		// https://www.cockroachlabs.com/docs/v22.1/transactions#transaction-contention ).
		context.commitAndBeginNewTransaction();
		now = clock.instant();
		self = findSelfExpectRegistered( context );
		// Delay expiration with each write
		self.setExpiration( now.plus( pulseExpiration ) );
		R instructions = pulseResult.applyAndReturnInstructions( now, self, agentPersister );
		log.tracef( "Agent '%s': ending pulse at %s with self = %s",
				selfReference(), now, self );
		return instructions;
	}

	private Agent ensureRegistered(AgentClusterLinkContext context) {
		Agent self = agentPersister.findSelf( context.agentRepository() );
		if ( self == null ) {
			Instant now = clock.instant();
			agentPersister.createSelf( context.agentRepository(), now.plus( pulseExpiration ) );
			// Make sure the transaction *only* registers the agent,
			// so that the risk of deadlocks (see below) is minimal
			// and other agents are made aware of this agent as soon as possible.
			// This avoids unnecessary rebalancing when multiple nodes start in quick succession.
			context.commitAndBeginNewTransaction();
			self = findSelfExpectRegistered( context );
		}
		return self;
	}

	private Agent findSelfExpectRegistered(AgentClusterLinkContext context) {
		Agent self = agentPersister.findSelf( context.agentRepository() );
		if ( self == null ) {
			throw log.agentRegistrationIneffective( selfReference() );
		}
		return self;
	}

	protected abstract WriteAction<R> doPulse(List<Agent> allAgentsInIdOrder, Agent self);

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

	public final void leaveCluster(AgentClusterLinkContext context) {
		agentPersister.leaveCluster( context.agentRepository() );
	}

	protected AgentReference selfReference() {
		return agentPersister.selfReference();
	}

	final AgentPersister getAgentPersisterForTests() {
		return agentPersister;
	}

	protected interface WriteAction<R> {
		R applyAndReturnInstructions(Instant now, Agent self, AgentPersister agentPersister);
	}
}
