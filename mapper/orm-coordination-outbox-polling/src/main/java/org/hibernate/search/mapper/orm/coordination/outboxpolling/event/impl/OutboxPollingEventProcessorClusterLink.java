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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentReference;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentRepository;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.EventProcessingState;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.jboss.logging.Logger;

public final class OutboxPollingEventProcessorClusterLink {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String agentName;
	private final FailureHandler failureHandler;
	private final Clock clock;
	private final OutboxEventFinderProvider finderProvider;
	private final Duration pulseInterval;
	private final Duration pulseExpiration;

	// Accessible for test purposes
	final boolean shardAssignmentIsStatic;
	AgentReference selfReference;
	ShardAssignment lastShardAssignment;

	public OutboxPollingEventProcessorClusterLink(String agentName,
			FailureHandler failureHandler, Clock clock, OutboxEventFinderProvider finderProvider,
			Duration pulseInterval, Duration pulseExpiration,
			ShardAssignmentDescriptor staticShardAssignment) {
		this.agentName = agentName;
		this.failureHandler = failureHandler;
		this.clock = clock;
		this.finderProvider = finderProvider;
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

	public AgentInstructions pulse(AgentRepository agentRepository) {
		Instant now = clock.instant();

		// In order to avoid transaction deadlocks with some RDBMS (yes, I mean SQL Server),
		// we make sure that *all* reads (listing all agents) happen before the write (creating/updating self).
		// I'm not entirely sure, but I think it goes like this:
		// if we read, then write, then read again, then the second read can trigger a deadlock,
		// with each transaction holding a write lock on some rows
		// while waiting for a read lock on the whole table.
		List<Agent> allAgentsInIdOrder = agentRepository.findAllOrderById();
		Agent self = createOrUpdateSelf( agentRepository, allAgentsInIdOrder, now );
		log.tracef( "Agent '%s': starting pulse at %s with self = %s, all agents = %s",
				selfReference, now, self, allAgentsInIdOrder );

		List<Agent> timedOutAgents = allAgentsInIdOrder.stream()
				.filter( a -> a.getExpiration().isBefore( now ) )
				.collect( Collectors.toList() );
		if ( !timedOutAgents.isEmpty() ) {
			log.removingTimedOutAgents( selfReference, timedOutAgents );
			agentRepository.delete( timedOutAgents );
			log.infof( "Agent '%s': suspending the agent to assess the new situation in the next pulse",
					selfReference, now );
			setSuspended( self );
			return instructCommitAndRetryPulseASAP( now );
		}

		// The target must be completely independent of the current in-memory state of this agent,
		// so that every agent will conclude
		ClusterTarget clusterTarget;
		try {
			clusterTarget = ClusterTarget.create( allAgentsInIdOrder );
		}
		catch (SearchException e) {
			FailureContext.Builder contextBuilder = FailureContext.builder();
			contextBuilder.throwable( log.outboxEventProcessorPulseFailed( selfReference, e.getMessage(),
					allAgentsInIdOrder, e ) );
			contextBuilder.failingOperation( log.outboxEventProcessorPulse( selfReference ) );
			failureHandler.handle( contextBuilder.build() );
			setSuspended( self );
			return instructCommitAndRetryPulseAfterInterval( now );
		}

		Optional<ShardAssignmentDescriptor> shardAssignmentOptional =
				ShardAssignmentDescriptor.fromClusterMemberList( clusterTarget.descriptor.memberIdsInShardOrder, selfReference.id );
		if ( !shardAssignmentOptional.isPresent() ) {
			log.logf( self.getState() != EventProcessingState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
					"Agent '%s': this agent is superfluous and will not perform event processing,"
							+ " because other agents are enough to handle all the shards."
							+ " Target cluster: %s.",
					selfReference, clusterTarget.descriptor );
			setSuspended( self );
			return instructCommitAndRetryPulseAfterInterval( now );
		}

		ShardAssignmentDescriptor targetShardAssignment = shardAssignmentOptional.get();

		if ( clusterTarget.descriptor.memberIdsInShardOrder.contains( null ) ) {
			log.logf( self.getState() != EventProcessingState.SUSPENDED ? Logger.Level.INFO : Logger.Level.TRACE,
					"Agent '%s': some cluster members are missing; this agent will wait until they are present."
							+ " Target cluster: %s.",
					selfReference, clusterTarget.descriptor );
			setSuspended( self );
			return instructCommitAndRetryPulseASAP( now );
		}

		ShardAssignmentDescriptor persistedShardAssignment = self.getShardAssignment();

		if ( !targetShardAssignment.equals( persistedShardAssignment ) ) {
			log.infof( "Agent '%s': the persisted shard assignment (%s) does not match the target."
							+ " Target assignment: %s."
							+ " Cluster: %s.",
					selfReference, persistedShardAssignment, targetShardAssignment,
					clusterTarget.descriptor );
			setRebalancing( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
		}

		// Check whether excluded (superfluous) agents complied with the cluster target and suspended themselves.
		if ( !excludedAgentsAreOutOfCluster( clusterTarget.excluded ) ) {
			setRebalancing( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
		}

		// Check whether cluster members complied with the cluster target.
		// Note this also checks the Agent instance representing self,
		// which ensures that we won't just spawn the agent directly in the RUNNING state,
		// but will always persist it once in the SUSPENDED state first,
		// making others aware of our existence before we start running.
		// Failing that, two agents spawning concurrently could potentially
		// start their own, separate cluster (split brain).
		// By requiring at least two transaction to switch from "just spawned" to RUNNING,
		// we make sure that on the second transaction,
		// one of those agent would see the other and take it into account when rebalancing.
		if ( !clusterMembersAreInCluster( clusterTarget.membersInShardOrder, clusterTarget.descriptor ) ) {
			setRebalancing( self, clusterTarget.descriptor, targetShardAssignment );
			return instructCommitAndRetryPulseASAP( now );
		}

		// If all the conditions above are satisfied, then we can start processing.
		setRunning( self, clusterTarget.descriptor, targetShardAssignment );
		return instructProceedWithEventProcessing( now );
	}

	public void leaveCluster(AgentRepository store) {
		if ( selfReference == null ) {
			// We never even joined the cluster
			return;
		}
		log.infof( "Agent '%s': leaving cluster", selfReference );
		Agent agent = store.find( selfReference.id );
		if ( agent != null ) {
			store.delete( Collections.singletonList( agent ) );
		}
	}

	private Agent createOrUpdateSelf(AgentRepository agentRepository, List<Agent> allAgentsInIdOrder, Instant now) {
		Instant nextExpiration = now.plus( pulseExpiration );
		Agent self = null;
		if ( selfReference != null ) {
			for ( Agent agent : allAgentsInIdOrder ) {
				if ( agent.getId().equals( selfReference.id ) ) {
					self = agent;
					break;
				}
			}
		}
		if ( self == null ) {
			AgentType type;
			ShardAssignmentDescriptor shardAssignment;
			if ( shardAssignmentIsStatic ) {
				type = AgentType.EVENT_PROCESSING_STATIC_SHARDING;
				shardAssignment = lastShardAssignment.descriptor;
			}
			else {
				type = AgentType.EVENT_PROCESSING_DYNAMIC_SHARDING;
				shardAssignment = null;
			}
			self = new Agent( type, agentName, nextExpiration, EventProcessingState.SUSPENDED, shardAssignment );
			agentRepository.create( self );
			selfReference = self.getReference();
			ListIterator<Agent> it = allAgentsInIdOrder.listIterator();
			// Find the position where self should be inserted
			while ( it.hasNext() ) {
				if ( it.next().getId() >= self.getId() ) {
					if ( it.hasPrevious() ) {
						it.previous();
					}
					break;
				}
			}
			// Insert self
			it.add( self );
		}
		else {
			self.setExpiration( nextExpiration );
		}
		return self;
	}

	private boolean excludedAgentsAreOutOfCluster(List<Agent> excludedAgents) {
		EventProcessingState expectedState = EventProcessingState.SUSPENDED;
		for ( Agent agent : excludedAgents ) {
			if ( !expectedState.equals( agent.getState() ) ) {
				log.tracef( "Agent '%s': waiting for agent '%s', which has not reached state '%s' yet",
						selfReference, agent.getReference(), expectedState );
				return false;
			}
		}

		log.tracef( "Agent '%s': agents excluded from the cluster reached the expected state %s",
				selfReference, expectedState );
		return true;
	}

	private boolean clusterMembersAreInCluster(List<Agent> clusterMembersInShardOrder,
			ClusterDescriptor clusterDescriptor) {
		int expectedTotalShardCount = clusterMembersInShardOrder.size();
		int expectedAssignedShardIndex = 0;
		Set<EventProcessingState> expectedStates = EventProcessingState.REBALANCING_OR_RUNNING;
		for ( Agent agent : clusterMembersInShardOrder ) {
			EventProcessingState state = agent.getState();
			if ( !expectedStates.contains( agent.getState() ) ) {
				log.tracef( "Agent '%s': waiting for agent '%s', whose state %s is not in the expected %s yet",
						selfReference, agent.getReference(), state, expectedStates );
				return false;
			}
			Integer totalShardCount = agent.getTotalShardCount();
			if ( totalShardCount == null || expectedTotalShardCount != totalShardCount ) {
				log.tracef( "Agent '%s': waiting for agent '%s', whose total shard count %s is not the expected %s yet",
						selfReference, agent.getReference(), totalShardCount, expectedTotalShardCount );
				return false;
			}
			Integer assignedShardIndex = agent.getAssignedShardIndex();
			if ( assignedShardIndex == null || expectedAssignedShardIndex != assignedShardIndex ) {
				log.tracef( "Agent '%s': waiting for agent '%s', whose assigned shard index %s is not the expected %s yet",
						selfReference, agent.getReference(), assignedShardIndex, expectedAssignedShardIndex );
				return false;
			}
			++expectedAssignedShardIndex;
		}

		log.tracef( "Agent '%s': all cluster members reached the expected states %s and shard assignment %s",
				selfReference, expectedStates, clusterDescriptor );
		return true;
	}

	private void setSuspended(Agent self) {
		if ( self.getState() != EventProcessingState.SUSPENDED ) {
			log.infof( "Agent '%s': suspending", selfReference );
			self.setState( EventProcessingState.SUSPENDED );
		}
		if ( !shardAssignmentIsStatic ) {
			self.setTotalShardCount( null );
			self.setAssignedShardIndex( null );
		}
	}

	private void setRebalancing(Agent self, ClusterDescriptor clusterDescriptor,
			ShardAssignmentDescriptor shardAssignment) {
		if ( self.getState() != EventProcessingState.REBALANCING ) {
			log.infof( "Agent '%s': rebalancing. Shard assignment: %s. Cluster: %s",
					selfReference, shardAssignment, clusterDescriptor );
			self.setState( EventProcessingState.REBALANCING );
		}
		if ( !shardAssignmentIsStatic ) {
			self.setTotalShardCount( shardAssignment.totalShardCount );
			self.setAssignedShardIndex( shardAssignment.assignedShardIndex );
		}
	}

	private void setRunning(Agent self, ClusterDescriptor clusterDescriptor,
			ShardAssignmentDescriptor shardAssignment) {
		if ( lastShardAssignment == null || !shardAssignment.equals( lastShardAssignment.descriptor ) ) {
			if ( shardAssignmentIsStatic ) {
				throw new AssertionFailure( "Agent '" + selfReference + "' has a static shard assignment,"
						+ " but the target shard assignment"
						+ " (" + shardAssignment + ")"
						+ " does not match the static shard assignment"
						+ " (" + lastShardAssignment + ")" );
			}
			log.infof( "Agent '%s': assigning to %s", selfReference, shardAssignment );
			this.lastShardAssignment = ShardAssignment.of( shardAssignment, finderProvider );
		}

		if ( self.getState() != EventProcessingState.RUNNING ) {
			log.infof( "Agent '%s': running. Shard assignment: %s. Cluster: %s",
					selfReference, shardAssignment, clusterDescriptor );
			self.setState( EventProcessingState.RUNNING );
		}
	}

	private AgentInstructions instructCommitAndRetryPulseASAP(Instant now) {
		log.tracef( "Agent '%s': instructions are to not process events and to retry a pulse as soon as possible",
				selfReference );
		return new AgentInstructions( clock, now, Optional.empty() );
	}

	private AgentInstructions instructCommitAndRetryPulseAfterInterval(Instant now) {
		log.tracef( "Agent '%s': instructions are to not process events and to retry a pulse in %s",
				selfReference, pulseInterval );
		return new AgentInstructions( clock, now.plus( pulseInterval ), Optional.empty() );
	}

	private AgentInstructions instructProceedWithEventProcessing(Instant now) {
		log.tracef( "Agent '%s': instructions are to process events and to retry a pulse in %s",
				selfReference, pulseInterval );
		return new AgentInstructions( clock, now.plus( pulseInterval ),
				Optional.of( lastShardAssignment.eventFinder ) );
	}

}
