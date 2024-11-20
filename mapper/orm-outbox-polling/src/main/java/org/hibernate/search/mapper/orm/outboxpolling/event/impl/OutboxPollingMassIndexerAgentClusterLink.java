/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;

public final class OutboxPollingMassIndexerAgentClusterLink
		extends AbstractAgentClusterLink<OutboxPollingMassIndexingInstructions> {

	private static final ClusterDescriptor SINGLE_NODE_CLUSTER_DESCRIPTOR = null;
	private static final ShardAssignmentDescriptor SINGLE_NODE_SHARD_ASSIGNMENT = null;

	public OutboxPollingMassIndexerAgentClusterLink(String agentName,
			FailureHandler failureHandler, Clock clock,
			Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration) {
		super(
				new AgentPersister( AgentType.MASS_INDEXING, agentName, null ),
				failureHandler, clock,
				pollingInterval, pulseInterval, pulseExpiration
		);
	}

	@Override
	protected WriteAction<OutboxPollingMassIndexingInstructions> doPulse(List<Agent> allAgentsInIdOrder, Agent currentSelf) {
		List<Agent> eventProcessors = allAgentsInIdOrder.stream()
				.filter( a -> AgentType.EVENT_PROCESSING.contains( a.getType() ) )
				.collect( Collectors.toList() );

		// Check whether event processors acknowledged our existence by suspending themselves.
		if ( !eventProcessorsAreSuspended( eventProcessors ) ) {
			return (now, self, agentPersister) -> {
				agentPersister.setWaiting( self, SINGLE_NODE_CLUSTER_DESCRIPTOR, SINGLE_NODE_SHARD_ASSIGNMENT );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		// Ensure that we won't just spawn the agent directly in the RUNNING state,
		// but will always persist it once in the WAITING state first,
		// making others aware of our existence before we start running.
		// Failing that, an event processor spawning concurrently to the mass indexer could potentially
		// start processing events before seeing the mass indexer,
		// while the mass indexer starts mass indexing without seeing the event processor.
		// By requiring at least two transactions to switch from "just spawned" to RUNNING,
		// we make sure that on the second transaction,
		// one of those agents would see the other and take it into account.
		if ( AgentState.SUSPENDED.equals( currentSelf.getState() ) ) {
			return (now, self, agentPersister) -> {
				agentPersister.setWaiting( self, SINGLE_NODE_CLUSTER_DESCRIPTOR, SINGLE_NODE_SHARD_ASSIGNMENT );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		// If all the conditions above are satisfied, then we can start mass indexing.
		return (now, self, agentPersister) -> {
			agentPersister.setRunning( self, SINGLE_NODE_CLUSTER_DESCRIPTOR );
			return instructProceedWithMassIndexing( now );
		};
	}

	private boolean eventProcessorsAreSuspended(List<Agent> eventProcessors) {
		AgentState expectedState = AgentState.SUSPENDED;
		for ( Agent eventProcessor : eventProcessors ) {
			if ( !expectedState.equals( eventProcessor.getState() ) ) {
				OutboxPollingEventsLog.INSTANCE.agentWaitingForEvents( selfReference(), eventProcessor.getReference(),
						expectedState );
				return false;
			}
		}

		OutboxPollingEventsLog.INSTANCE.agentProcessorsExpired( selfReference(), expectedState );
		return true;
	}

	@Override
	protected OutboxPollingMassIndexingInstructions instructCommitAndRetryPulseAfterDelay(Instant now, Duration delay) {
		Instant expiration = now.plus( delay );
		OutboxPollingEventsLog.INSTANCE.agentHoldMassIndexing( selfReference(), delay, expiration );
		return new OutboxPollingMassIndexingInstructions( clock, expiration, false );
	}

	private OutboxPollingMassIndexingInstructions instructProceedWithMassIndexing(Instant now) {
		Instant expiration = now.plus( pulseInterval );
		OutboxPollingEventsLog.INSTANCE.agentProceedMassIndexing( selfReference(), pulseInterval, expiration );
		return new OutboxPollingMassIndexingInstructions( clock, expiration, true );
	}

}
