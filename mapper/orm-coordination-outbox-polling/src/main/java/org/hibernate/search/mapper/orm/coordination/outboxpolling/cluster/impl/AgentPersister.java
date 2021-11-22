/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class AgentPersister {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final AgentType type;
	private final String name;
	private final ShardAssignmentDescriptor staticShardAssignment;

	private AgentReference selfReference;

	public AgentPersister(AgentType type, String name,
			ShardAssignmentDescriptor staticShardAssignment) {
		this.type = type;
		this.name = name;
		this.staticShardAssignment = staticShardAssignment;
	}

	public AgentReference selfReference() {
		return selfReference;
	}

	// Accessible for test purposes only
	public void setSelfReferenceForTests(AgentReference selfReference) {
		this.selfReference = selfReference;
	}

	public Agent extractSelf(List<Agent> allAgentsInIdOrder) {
		if ( selfReference != null ) {
			for ( Agent agent : allAgentsInIdOrder ) {
				if ( agent.getId().equals( selfReference.id ) ) {
					return agent;
				}
			}
		}
		return null;
	}

	public Agent createSelf(AgentRepository agentRepository, List<Agent> allAgentsInIdOrder, Instant expiration) {
		Agent self = new Agent( type, name, expiration, EventProcessingState.SUSPENDED, staticShardAssignment );
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
		return self;
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

	public void setSuspended(Agent self) {
		if ( self.getState() != EventProcessingState.SUSPENDED ) {
			log.infof( "Agent '%s': suspending", selfReference );
			self.setState( EventProcessingState.SUSPENDED );
		}
		if ( staticShardAssignment == null ) {
			self.setTotalShardCount( null );
			self.setAssignedShardIndex( null );
		}
	}

	public void setRebalancing(Agent self, ClusterDescriptor clusterDescriptor,
			ShardAssignmentDescriptor shardAssignment) {
		if ( self.getState() != EventProcessingState.REBALANCING ) {
			log.infof( "Agent '%s': rebalancing. Shard assignment: %s. Cluster: %s",
					selfReference, shardAssignment, clusterDescriptor );
			self.setState( EventProcessingState.REBALANCING );
		}
		if ( staticShardAssignment == null ) {
			self.setTotalShardCount( shardAssignment.totalShardCount );
			self.setAssignedShardIndex( shardAssignment.assignedShardIndex );
		}
	}

	public void setRunning(Agent self, ClusterDescriptor clusterDescriptor) {
		if ( self.getState() != EventProcessingState.RUNNING ) {
			log.infof( "Agent '%s': running. Shard assignment: %s. Cluster: %s",
					selfReference, self.getShardAssignment(), clusterDescriptor );
			self.setState( EventProcessingState.RUNNING );
		}
	}

}
