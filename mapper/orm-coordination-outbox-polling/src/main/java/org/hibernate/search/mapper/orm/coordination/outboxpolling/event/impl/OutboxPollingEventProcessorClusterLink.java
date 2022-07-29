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
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.jboss.logging.Logger;

public final class OutboxPollingEventProcessorClusterLink
		extends AbstractAgentClusterLink<OutboxPollingEventProcessingInstructions> {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final OutboxEventFinderProvider finderProvider;

	// Accessible for test purposes
	final boolean shardAssignmentIsStatic;
	ShardAssignment lastShardAssignment;

	public OutboxPollingEventProcessorClusterLink(String agentName,
			FailureHandler failureHandler, Clock clock, OutboxEventFinderProvider finderProvider,
			Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration,
			ShardAssignmentDescriptor staticShardAssignment) {
		super(
				new AgentPersister(
						staticShardAssignment == null ? AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING
								: AgentType.EVENT_PROCESSING_STATIC_SHARDING,
						agentName, staticShardAssignment
				),
				failureHandler, clock,
				pollingInterval, pulseInterval, pulseExpiration
		);
		this.finderProvider = finderProvider;

		if ( staticShardAssignment == null ) {
			this.shardAssignmentIsStatic = false;
			this.lastShardAssignment = null;
		}
		else {
			this.shardAssignmentIsStatic = true;
			this.lastShardAssignment = ShardAssignment.of( staticShardAssignment, finderProvider );
		}
		log.tracef( "Agent '%s': created, staticShardAssignment = %s",
				agentName, staticShardAssignment );
	}

	@Override
	protected OutboxPollingEventProcessingInstructions doPulse(AgentRepository agentRepository, Instant now,
			List<Agent> allAgentsInIdOrder, Agent self) {
		for ( Agent agent : allAgentsInIdOrder ) {
			if ( AgentType.MASS_INDEXING.equals( agent.getType() ) ) {
				log.logf( self.getState() != AgentState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
						"Agent '%s': another agent '%s' is currently mass indexing",
						selfReference(), now, agent );
				agentPersister.setSuspended( self );
				return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
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
			contextBuilder.throwable( log.outboxEventProcessorPulseFailed( selfReference(), e.getMessage(),
					allAgentsInIdOrder, e ) );
			contextBuilder.failingOperation( log.outboxEventProcessorPulse( selfReference() ) );
			failureHandler.handle( contextBuilder.build() );
			agentPersister.setSuspended( self );
			return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
		}

		Optional<ShardAssignmentDescriptor> shardAssignmentOptional =
				ShardAssignmentDescriptor.fromClusterMemberList( clusterTarget.descriptor.memberIdsInShardOrder, selfReference().id );
		if ( !shardAssignmentOptional.isPresent() ) {
			log.logf( self.getState() != AgentState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
					"Agent '%s': this agent is superfluous and will not perform event processing,"
							+ " because other agents are enough to handle all the shards."
							+ " Target cluster: %s.",
					selfReference(), clusterTarget.descriptor );
			agentPersister.setSuspended( self );
			return instructCommitAndRetryPulseAfterDelay( now, pulseInterval );
		}

		ShardAssignmentDescriptor targetShardAssignment = shardAssignmentOptional.get();

		if ( clusterTarget.descriptor.memberIdsInShardOrder.contains( null ) ) {
			log.logf( self.getState() != AgentState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
					"Agent '%s': some cluster members are missing; this agent will wait until they are present."
							+ " Target cluster: %s.",
					selfReference(), clusterTarget.descriptor );
			agentPersister.setSuspended( self );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
		}

		ShardAssignmentDescriptor persistedShardAssignment = self.getShardAssignment();

		if ( !targetShardAssignment.equals( persistedShardAssignment ) ) {
			log.infof( "Agent '%s': the persisted shard assignment (%s) does not match the target."
							+ " Target assignment: %s."
							+ " Cluster: %s.",
					selfReference(), persistedShardAssignment, targetShardAssignment,
					clusterTarget.descriptor );
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
		}

		// Check whether excluded (superfluous) agents complied with the cluster target and suspended themselves.
		if ( !excludedAgentsAreOutOfCluster( clusterTarget.excluded ) ) {
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
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
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseAfterDelay( now, pollingInterval );
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
			log.infof( "Agent '%s': assigning to %s", selfReference(), targetShardAssignment );
			this.lastShardAssignment = ShardAssignment.of( targetShardAssignment, finderProvider );
		}
		agentPersister.setRunning( self, clusterTarget.descriptor );
		return instructProceedWithEventProcessing( now );
	}

	private boolean excludedAgentsAreOutOfCluster(List<Agent> excludedAgents) {
		AgentState expectedState = AgentState.SUSPENDED;
		for ( Agent agent : excludedAgents ) {
			if ( !expectedState.equals( agent.getState() ) ) {
				log.tracef( "Agent '%s': waiting for agent '%s', which has not reached state '%s' yet",
						selfReference(), agent.getReference(), expectedState );
				return false;
			}
		}

		log.tracef( "Agent '%s': agents excluded from the cluster reached the expected state %s",
				selfReference(), expectedState );
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
				log.tracef( "Agent '%s': waiting for agent '%s', whose state %s is not in the expected %s yet",
						selfReference(), agent.getReference(), state, expectedStates );
				return false;
			}
			Integer totalShardCount = agent.getTotalShardCount();
			if ( totalShardCount == null || expectedTotalShardCount != totalShardCount ) {
				log.tracef( "Agent '%s': waiting for agent '%s', whose total shard count %s is not the expected %s yet",
						selfReference(), agent.getReference(), totalShardCount, expectedTotalShardCount );
				return false;
			}
			Integer assignedShardIndex = agent.getAssignedShardIndex();
			if ( assignedShardIndex == null || expectedAssignedShardIndex != assignedShardIndex ) {
				log.tracef( "Agent '%s': waiting for agent '%s', whose assigned shard index %s is not the expected %s yet",
						selfReference(), agent.getReference(), assignedShardIndex, expectedAssignedShardIndex );
				return false;
			}
			++expectedAssignedShardIndex;
		}

		log.tracef( "Agent '%s': all cluster members reached the expected states %s and shard assignment %s",
				selfReference(), expectedStates, clusterDescriptor );
		return true;
	}

	@Override
	protected OutboxPollingEventProcessingInstructions instructCommitAndRetryPulseAfterDelay(Instant now, Duration delay) {
		Instant expiration = now.plus( delay );
		log.tracef( "Agent '%s': instructions are to not process events and to retry a pulse in %s, around %s",
				selfReference(), delay, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.empty() );
	}

	private OutboxPollingEventProcessingInstructions instructProceedWithEventProcessing(Instant now) {
		Instant expiration = now.plus( pulseInterval );
		log.tracef( "Agent '%s': instructions are to process events and to retry a pulse in %s, around %s",
				selfReference(), pulseInterval, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.of( lastShardAssignment.eventFinder ) );
	}

}
