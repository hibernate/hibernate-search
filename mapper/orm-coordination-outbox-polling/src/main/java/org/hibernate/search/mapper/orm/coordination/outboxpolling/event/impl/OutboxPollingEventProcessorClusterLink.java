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
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentPersister;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference;
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

public final class OutboxPollingEventProcessorClusterLink {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FailureHandler failureHandler;
	private final Clock clock;
	private final OutboxEventFinderProvider finderProvider;
	private final Duration pollingInterval;
	private final Duration pulseInterval;
	private final Duration pulseExpiration;

	// Accessible for test purposes
	final AgentPersister agentPersister;
	final boolean shardAssignmentIsStatic;
	ShardAssignment lastShardAssignment;

	public OutboxPollingEventProcessorClusterLink(String agentName,
			FailureHandler failureHandler, Clock clock, OutboxEventFinderProvider finderProvider,
			Duration pollingInterval, Duration pulseInterval, Duration pulseExpiration,
			ShardAssignmentDescriptor staticShardAssignment) {
		this.agentPersister =
				new AgentPersister(
						staticShardAssignment == null ? AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING
								: AgentType.EVENT_PROCESSING_STATIC_SHARDING,
						agentName, staticShardAssignment
				);
		this.failureHandler = failureHandler;
		this.clock = clock;
		this.finderProvider = finderProvider;
		this.pollingInterval = pollingInterval;
		this.pulseInterval = pulseInterval;
		this.pulseExpiration = pulseExpiration;

		if ( staticShardAssignment == null ) {
			this.shardAssignmentIsStatic = false;
			this.lastShardAssignment = null;
		}
		else {
			this.shardAssignmentIsStatic = true;
			this.lastShardAssignment = ShardAssignment.of( staticShardAssignment, finderProvider );
		}
		log.tracef( "Agent '%s': staticShardAssignment = %s",
				agentName, staticShardAssignment );
	}

	public OutboxPollingEventProcessingInstructions pulse(AgentRepository agentRepository) {
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
			return instructCommitAndRetryPulseASAP( now );
		}

		// Delay expiration in each pulse
		self.setExpiration( newExpiration );

		// The target must be completely independent of the current in-memory state of this agent,
		// so that every agent will conclude
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
			return instructCommitAndRetryPulseAfterInterval( now );
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
			return instructCommitAndRetryPulseAfterInterval( now );
		}

		ShardAssignmentDescriptor targetShardAssignment = shardAssignmentOptional.get();

		if ( clusterTarget.descriptor.memberIdsInShardOrder.contains( null ) ) {
			log.logf( self.getState() != AgentState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
					"Agent '%s': some cluster members are missing; this agent will wait until they are present."
							+ " Target cluster: %s.",
					selfReference(), clusterTarget.descriptor );
			agentPersister.setSuspended( self );
			return instructCommitAndRetryPulseASAP( now );
		}

		ShardAssignmentDescriptor persistedShardAssignment = self.getShardAssignment();

		if ( !targetShardAssignment.equals( persistedShardAssignment ) ) {
			log.infof( "Agent '%s': the persisted shard assignment (%s) does not match the target."
							+ " Target assignment: %s."
							+ " Cluster: %s.",
					selfReference(), persistedShardAssignment, targetShardAssignment,
					clusterTarget.descriptor );
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
		}

		// Check whether excluded (superfluous) agents complied with the cluster target and suspended themselves.
		if ( !excludedAgentsAreOutOfCluster( clusterTarget.excluded ) ) {
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
		}

		// Check whether cluster members complied with the cluster target.
		// Note this also checks the Agent instance representing self,
		// which ensures that we won't just spawn the agent directly in the RUNNING state,
		// but will always persist it once in the WAITING state first,
		// making others aware of our existence before we start running.
		// Failing that, two agents spawning concurrently could potentially
		// start their own, separate cluster (split brain).
		// By requiring at least two transaction to switch from "just spawned" to RUNNING,
		// we make sure that on the second transaction,
		// one of those agent would see the other and take it into account when rebalancing.
		if ( !clusterMembersAreInCluster( clusterTarget.membersInShardOrder, clusterTarget.descriptor ) ) {
			agentPersister.setWaiting( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
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

	public void leaveCluster(AgentRepository agentRepository) {
		agentPersister.leaveCluster( agentRepository );
	}

	private AgentReference selfReference() {
		return agentPersister.selfReference();
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

	private OutboxPollingEventProcessingInstructions instructCommitAndRetryPulseASAP(Instant now) {
		Instant expiration = now.plus( pollingInterval );
		log.tracef( "Agent '%s': instructions are to not process events and to retry a pulse in %s, around %s",
				selfReference(), pollingInterval, expiration );
		// "As soon as possible" still means we wait for a polling interval,
		// to avoid polling the database continuously.
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.empty() );
	}

	private OutboxPollingEventProcessingInstructions instructCommitAndRetryPulseAfterInterval(Instant now) {
		Instant expiration = now.plus( pulseInterval );
		log.tracef( "Agent '%s': instructions are to not process events and to retry a pulse in %s, around %s",
				selfReference(), pulseInterval, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.empty() );
	}

	private OutboxPollingEventProcessingInstructions instructProceedWithEventProcessing(Instant now) {
		Instant expiration = now.plus( pulseInterval );
		log.tracef( "Agent '%s': instructions are to process events and to retry a pulse in %s, around %s",
				selfReference(), pulseInterval, expiration );
		return new OutboxPollingEventProcessingInstructions( clock, expiration, Optional.of( lastShardAssignment.eventFinder ) );
	}

}
