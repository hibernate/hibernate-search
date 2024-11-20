/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class OutboxPollingEventProcessorClusterLink
		extends AbstractAgentClusterLink<OutboxPollingEventProcessingInstructions> {

	private final ShardAssignment.Provider shardAssignmentProvider;

	// Accessible for test purposes
	final boolean shardAssignmentIsStatic;
	ShardAssignment lastShardAssignment;

	public OutboxPollingEventProcessorClusterLink(String agentName,
			FailureHandler failureHandler, Clock clock, ShardAssignment.Provider shardAssignmentProvider,
			Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration,
			ShardAssignmentDescriptor staticShardAssignment) {
		super(
				new AgentPersister(
						staticShardAssignment == null
								? AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING
								: AgentType.EVENT_PROCESSING_STATIC_SHARDING,
						agentName, staticShardAssignment
				),
				failureHandler, clock,
				pollingInterval, pulseInterval, pulseExpiration
		);
		this.shardAssignmentProvider = shardAssignmentProvider;

		if ( staticShardAssignment == null ) {
			this.shardAssignmentIsStatic = false;
			this.lastShardAssignment = null;
		}
		else {
			this.shardAssignmentIsStatic = true;
			this.lastShardAssignment = shardAssignmentProvider.create( staticShardAssignment );
		}
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "shardAssignmentProvider", shardAssignmentProvider )
				.attribute( "shardAssignmentIsStatic", shardAssignmentIsStatic );
	}

	@Override
	protected WriteAction<OutboxPollingEventProcessingInstructions> doPulse(List<Agent> allAgentsInIdOrder, Agent currentSelf) {
		for ( Agent agent : allAgentsInIdOrder ) {
			if ( AgentType.MASS_INDEXING.equals( agent.getType() ) ) {
				if ( currentSelf.getState() != AgentState.SUSPENDED ) {
					OutboxPollingEventsLog.INSTANCE.agentOtherAgentIsIndexingInfo( selfReference(), agent );
				}
				else {
					OutboxPollingEventsLog.INSTANCE.agentOtherAgentIsIndexingTrace( selfReference(), agent );
				}
				return (now, self, agentPersister) -> {
					agentPersister.setSuspended( self );
					return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
				};
			}
		}

		// The target must be completely independent of the current in-memory state of this agent,
		// so that every agent will generate the same target
		// when reading the same information from the database.
		ClusterTarget clusterTarget;
		try {
			clusterTarget = ClusterTarget.create( allAgentsInIdOrder );
		}
		catch (SearchException e) {
			FailureContext.Builder contextBuilder = FailureContext.builder();
			contextBuilder
					.throwable(
							OutboxPollingEventsLog.INSTANCE.outboxEventProcessorPulseFailed( selfReference(), e.getMessage(),
									allAgentsInIdOrder, e ) );
			contextBuilder.failingOperation( OutboxPollingEventsLog.INSTANCE.outboxEventProcessorPulse( selfReference() ) );
			failureHandler.handle( contextBuilder.build() );
			return (now, self, agentPersister) -> {
				agentPersister.setSuspended( self );
				return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
			};
		}

		Optional<ShardAssignmentDescriptor> shardAssignmentOptional =
				ShardAssignmentDescriptor.fromClusterMemberList( clusterTarget.descriptor.memberIdsInShardOrder,
						selfReference().id );
		if ( shardAssignmentOptional.isEmpty() ) {
			if ( currentSelf.getState() != AgentState.SUSPENDED ) {
				OutboxPollingEventsLog.INSTANCE.agentSuperfluousInfo( selfReference(), clusterTarget.descriptor );
			}
			else {
				OutboxPollingEventsLog.INSTANCE.agentSuperfluousTrace( selfReference(), clusterTarget.descriptor );
			}
			return (now, self, agentPersister) -> {
				agentPersister.setSuspended( self );
				return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
			};
		}

		ShardAssignmentDescriptor targetShardAssignment = shardAssignmentOptional.get();

		if ( clusterTarget.descriptor.memberIdsInShardOrder.contains( null ) ) {
			if ( currentSelf.getState() != AgentState.SUSPENDED ) {
				OutboxPollingEventsLog.INSTANCE.agentClusterMembersMissingInfo( selfReference(), clusterTarget.descriptor );
			}
			else {
				OutboxPollingEventsLog.INSTANCE.agentClusterMembersMissingTrace( selfReference(), clusterTarget.descriptor );
			}
			return (now, self, agentPersister) -> {
				agentPersister.setSuspended( self );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		ShardAssignmentDescriptor persistedShardAssignment = currentSelf.getShardAssignment();

		if ( !targetShardAssignment.equals( persistedShardAssignment ) ) {
			OutboxPollingEventsLog.INSTANCE.agentAssignmentDoesNotMatchTarget(
					selfReference(), persistedShardAssignment, targetShardAssignment,
					clusterTarget.descriptor );
			return (now, self, agentPersister) -> {
				agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		// Check whether excluded (superfluous) agents complied with the cluster target and suspended themselves.
		if ( !excludedAgentsAreOutOfCluster( clusterTarget.excluded ) ) {
			return (now, self, agentPersister) -> {
				agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		// Check whether cluster members complied with the cluster target.
		// Note this also checks the Agent instance representing self,
		// which ensures that we won't just spawn the agent directly in the RUNNING state,
		// but will always persist it once in the WAITING state first,
		// making others aware of our existence before we start running.
		// Failing that, two agents spawning concurrently could potentially
		// start their own, separate cluster (split brain).
		// By requiring at least two transactions to switch from "just spawned" to RUNNING,
		// we make sure that on the second transaction,
		// one of those agents would see the other and take it into account when rebalancing.
		if ( !clusterMembersAreInCluster( clusterTarget.membersInShardOrder, clusterTarget.descriptor ) ) {
			return (now, self, agentPersister) -> {
				agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
				return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
			};
		}

		// If all the conditions above are satisfied, then we can start processing.
		if ( lastShardAssignment == null || !targetShardAssignment.equals( lastShardAssignment.descriptor ) ) {
			if ( shardAssignmentIsStatic ) {
				throw new AssertionFailure( "Agent '" + selfReference() + "' has a static shard assignment,"
						+ " but the target shard assignment"
						+ " (" + targetShardAssignment + ")"
						+ " does not match the static shard assignment"
						+ " (" + lastShardAssignment + ")" );
			}
			OutboxPollingEventsLog.INSTANCE.agentAssignment( selfReference(), targetShardAssignment );
			this.lastShardAssignment = shardAssignmentProvider.create( targetShardAssignment );
		}
		return (now, self, agentPersister) -> {
			agentPersister.setRunning( self, clusterTarget.descriptor );
			return instructProceedWithEventProcessing( now );
		};
	}

	private boolean excludedAgentsAreOutOfCluster(List<Agent> excludedAgents) {
		if ( excludedAgents.isEmpty() ) {
			return true;
		}

		AgentState expectedState = AgentState.SUSPENDED;
		for ( Agent agent : excludedAgents ) {
			if ( !expectedState.equals( agent.getState() ) ) {
				OutboxPollingEventsLog.INSTANCE.agentWaitingAgentReachState( selfReference(), agent.getReference(),
						expectedState );
				return false;
			}
		}

		OutboxPollingEventsLog.INSTANCE.agentExcluded( selfReference(), expectedState );
		return true;
	}

	private boolean clusterMembersAreInCluster(List<Agent> clusterMembersInShardOrder,
			ClusterDescriptor clusterDescriptor) {
		int expectedTotalShardCount = clusterMembersInShardOrder.size();
		int expectedAssignedShardIndex = 0;
		Set<AgentState> expectedStates = AgentState.WAITING_OR_RUNNING;
		for ( Agent agent : clusterMembersInShardOrder ) {
			AgentState state = agent.getState();
			if ( !expectedStates.contains( agent.getState() ) ) {
				OutboxPollingEventsLog.INSTANCE.clusterMembersAreInClusterWaitingForState( selfReference(),
						agent.getReference(),
						state, expectedStates );
				return false;
			}
			Integer totalShardCount = agent.getTotalShardCount();
			if ( totalShardCount == null || expectedTotalShardCount != totalShardCount ) {
				OutboxPollingEventsLog.INSTANCE.clusterMembersAreInClusterShardCountExpectation( selfReference(),
						agent.getReference(), totalShardCount, expectedTotalShardCount );
				return false;
			}
			Integer assignedShardIndex = agent.getAssignedShardIndex();
			if ( assignedShardIndex == null || expectedAssignedShardIndex != assignedShardIndex ) {
				OutboxPollingEventsLog.INSTANCE.clusterMembersAreInClusterSharIndexExpectation( selfReference(),
						agent.getReference(), assignedShardIndex, expectedAssignedShardIndex );
				return false;
			}
			++expectedAssignedShardIndex;
		}

		OutboxPollingEventsLog.INSTANCE.clusterMembersAreInClusterReachedExpectedStates( selfReference(), expectedStates,
				clusterDescriptor );
		return true;
	}

	@Override
	protected OutboxPollingEventProcessingInstructions instructCommitAndRetryPulseAfterDelay(Instant now, Duration delay) {
		Instant expiration = now.plus( delay );
		OutboxPollingEventsLog.INSTANCE.instructCommitAndRetryPulseAfterDelay( selfReference(), delay, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.empty() );
	}

	private OutboxPollingEventProcessingInstructions instructProceedWithEventProcessing(Instant now) {
		Instant expiration = now.plus( pulseInterval );
		OutboxPollingEventsLog.INSTANCE.instructProceedWithEventProcessing( selfReference(), pulseInterval, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration,
				Optional.of( lastShardAssignment.eventFinder ) );
	}

}
