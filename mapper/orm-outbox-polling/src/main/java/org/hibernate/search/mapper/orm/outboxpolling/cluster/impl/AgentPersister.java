/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.time.Instant;
import java.util.Collections;

import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingEventsLog;
import org.hibernate.search.util.common.spi.ToStringTreeAppendable;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

public final class AgentPersister implements ToStringTreeAppendable {

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
		return toStringTree();
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		appender.attribute( "type", type )
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
		OutboxPollingEventsLog.INSTANCE.infof( "Agent '%s': registering", selfReference );
	}

	public void leaveCluster(AgentRepository store) {
		if ( selfReference == null ) {
			// We never even joined the cluster
			return;
		}
		OutboxPollingEventsLog.INSTANCE.infof( "Agent '%s': leaving cluster", selfReference );
		Agent agent = store.find( selfReference.id );
		if ( agent != null ) {
			store.delete( Collections.singletonList( agent ) );
		}
	}

	public void setSuspended(Agent self) {
		if ( self.getState() != AgentState.SUSPENDED ) {
			OutboxPollingEventsLog.INSTANCE.infof( "Agent '%s': suspending", selfReference );
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
			OutboxPollingEventsLog.INSTANCE.infof( "Agent '%s': waiting for cluster changes. Shard assignment: %s. Cluster: %s",
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
			OutboxPollingEventsLog.INSTANCE.infof( "Agent '%s': running. Shard assignment: %s. Cluster: %s",
					selfReference, self.getShardAssignment(), clusterDescriptor );
			self.setState( AgentState.RUNNING );
		}
	}

}
