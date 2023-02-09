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

import org.hibernate.search.mapper.orm.coordination.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.impl.ToStringTreeAppendable;
import org.hibernate.search.util.common.impl.ToStringTreeBuilder;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class AgentPersister implements ToStringTreeAppendable {
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

	@Override
	public String toString() {
		return new ToStringTreeBuilder().value( this ).toString();
	}

	@Override
	public void appendTo(ToStringTreeBuilder builder) {
		builder.attribute( "type", type )
				.attribute( "name", name )
				.attribute( "staticShardAssignment", staticShardAssignment );
	}

	public AgentReference selfReference() {
		return selfReference;
	}

	// Accessible for test purposes only
	public void setSelfReferenceForTests(AgentReference selfReference) {
		this.selfReference = selfReference;
	}

	public Agent findSelf(AgentRepository agentRepository) {
		if ( selfReference != null ) {
			return agentRepository.find( selfReference.id );
		}
		return null;
	}

	public void createSelf(AgentRepository agentRepository, Instant expiration) {
		Agent self = new Agent( type, name, expiration, AgentState.SUSPENDED, staticShardAssignment );
		agentRepository.create( self );
		selfReference = self.getReference();
		log.infof( "Agent '%s': registering", selfReference );
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
		if ( self.getState() != AgentState.SUSPENDED ) {
			log.infof( "Agent '%s': suspending", selfReference );
			self.setState( AgentState.SUSPENDED );
		}
		if ( staticShardAssignment == null ) {
			self.setTotalShardCount( null );
			self.setAssignedShardIndex( null );
		}
	}

	public void setWaiting(Agent self, ClusterDescriptor clusterDescriptor,
			ShardAssignmentDescriptor shardAssignment) {
		if ( self.getState() != AgentState.WAITING ) {
			log.infof( "Agent '%s': waiting for cluster changes. Shard assignment: %s. Cluster: %s",
					selfReference, shardAssignment, clusterDescriptor );
			self.setState( AgentState.WAITING );
		}
		if ( staticShardAssignment == null && shardAssignment != null ) {
			self.setTotalShardCount( shardAssignment.totalShardCount );
			self.setAssignedShardIndex( shardAssignment.assignedShardIndex );
		}
	}

	public void setRunning(Agent self, ClusterDescriptor clusterDescriptor) {
		if ( self.getState() != AgentState.RUNNING ) {
			log.infof( "Agent '%s': running. Shard assignment: %s. Cluster: %s",
					selfReference, self.getShardAssignment(), clusterDescriptor );
			self.setState( AgentState.RUNNING );
		}
	}

}
