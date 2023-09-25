/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.outboxpolling.event.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.Agent;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.AgentType;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ClusterDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.mapper.orm.outboxpolling.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class ClusterTarget {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	static ClusterTarget create(List<Agent> allAgentsInIdOrder) {
		Agent[] candidatesInIdOrder = allAgentsInIdOrder.toArray( new Agent[0] );
		List<Agent> membersInShardOrder = new ArrayList<>();
		List<Agent> excluded = new ArrayList<>();

		assignStaticAgents( candidatesInIdOrder, membersInShardOrder );
		boolean hasStaticAgent = !membersInShardOrder.isEmpty();

		assignDynamicAgents( candidatesInIdOrder, hasStaticAgent, membersInShardOrder, excluded );

		return new ClusterTarget( membersInShardOrder, excluded );
	}

	private static void assignStaticAgents(Agent[] candidatesInIdOrder, List<Agent> membersInShardOrder) {
		Agent firstStaticAgent = null;
		Integer firstStaticAgentTotalShardCount = null;
		for ( int i = 0; i < candidatesInIdOrder.length; i++ ) {
			Agent agent = candidatesInIdOrder[i];
			if ( !AgentType.EVENT_PROCESSING_STATIC_SHARDING.equals( agent.getType() ) ) {
				continue;
			}
			ShardAssignmentDescriptor agentStaticShardAssignment = agent.getShardAssignment();
			int agentTotalShardCount = agentStaticShardAssignment.totalShardCount;
			if ( firstStaticAgentTotalShardCount == null ) {
				firstStaticAgent = agent;
				firstStaticAgentTotalShardCount = agentTotalShardCount;
				while ( membersInShardOrder.size() < firstStaticAgentTotalShardCount ) {
					membersInShardOrder.add( null );
				}
			}
			else if ( !firstStaticAgentTotalShardCount.equals( agentTotalShardCount ) ) {
				throw log.conflictingOutboxEventBackgroundProcessorAgentTotalShardCountForStaticSharding(
						agent.getReference(), agentStaticShardAssignment,
						firstStaticAgent.getReference(), firstStaticAgentTotalShardCount
				);
			}
			Agent previouslyAssigned = membersInShardOrder.set( agentStaticShardAssignment.assignedShardIndex, agent );
			if ( previouslyAssigned != null ) {
				throw log.conflictingOutboxEventBackgroundProcessorAgentShardsForStaticSharding(
						agent.getReference(), agentStaticShardAssignment, previouslyAssigned.getReference()
				);
			}
			candidatesInIdOrder[i] = null;
		}
	}

	private static void assignDynamicAgents(Agent[] candidatesInIdOrder, boolean hasStaticAgent,
			List<Agent> membersInShardOrder, List<Agent> excluded) {
		for ( int i = 0, j = 0; i < candidatesInIdOrder.length; i++ ) {
			if ( candidatesInIdOrder[i] == null ) {
				// This was a statically assigned agent, and it's already been assigned.
				continue;
			}
			// Step over statically assigned agents
			while ( j < membersInShardOrder.size() && membersInShardOrder.get( j ) != null ) {
				++j;
			}
			if ( j < membersInShardOrder.size() ) {
				// Static sharding, and we haven't reached the total shard count: we can insert a dynamic agent
				membersInShardOrder.set( j, candidatesInIdOrder[i] );
			}
			else if ( !hasStaticAgent ) {
				// Dynamic sharding: we can add as many agents as we want
				membersInShardOrder.add( candidatesInIdOrder[i] );
			}
			else {
				// Static sharding, and we've reached the total shard count: we cannot add any more agents
				excluded.add( candidatesInIdOrder[i] );
			}
		}
	}

	final List<Agent> membersInShardOrder;
	final List<Agent> excluded;
	final ClusterDescriptor descriptor;

	private ClusterTarget(List<Agent> membersInShardOrder, List<Agent> excluded) {
		this.membersInShardOrder = Collections.unmodifiableList( membersInShardOrder );
		this.excluded = Collections.unmodifiableList( excluded );
		List<UUID> memberIdsInShardOrder = new ArrayList<>( membersInShardOrder.size() );
		for ( Agent member : membersInShardOrder ) {
			memberIdsInShardOrder.add( member == null ? null : member.getId() );
		}
		descriptor = new ClusterDescriptor( memberIdsInShardOrder );
	}

}
