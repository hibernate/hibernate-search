/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Transient;

public class Agent {
	private UUID id;
	private AgentType type;
	private String name;
	private Instant expiration;
	private AgentState state;
	private Integer totalShardCount;
	private Integer assignedShardIndex;
	private byte[] payload;

	protected Agent() {
	}

	public Agent(AgentType type, String name, Instant expiration, AgentState state,
			ShardAssignmentDescriptor shardAssignment) {
		this.type = type;
		this.name = name;
		this.expiration = expiration;
		this.state = state;
		this.totalShardCount = shardAssignment == null ? null : shardAssignment.totalShardCount;
		this.assignedShardIndex = shardAssignment == null ? null : shardAssignment.assignedShardIndex;
	}

	@Override
	public String toString() {
		return "Agent{" +
				"id=" + id +
				", name='" + name + '\'' +
				", expiration=" + expiration +
				", type=" + type +
				", currentState=" + state +
				", totalShardCount=" + totalShardCount +
				", assignedShardIndex=" + assignedShardIndex +
				'}';
	}

	public UUID getId() {
		return id;
	}

	// For tests only
	public void setId(UUID id) {
		this.id = id;
	}

	public AgentType getType() {
		return type;
	}

	public void setType(AgentType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public Instant getExpiration() {
		return expiration;
	}

	public void setExpiration(Instant expiration) {
		this.expiration = expiration;
	}

	public AgentState getState() {
		return state;
	}

	public void setState(AgentState state) {
		this.state = state;
	}

	public Integer getTotalShardCount() {
		return totalShardCount;
	}

	public void setTotalShardCount(Integer totalShardCount) {
		this.totalShardCount = totalShardCount;
	}

	public Integer getAssignedShardIndex() {
		return assignedShardIndex;
	}

	public void setAssignedShardIndex(Integer assignedShardIndex) {
		this.assignedShardIndex = assignedShardIndex;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Transient
	public AgentReference getReference() {
		return AgentReference.of( id, name );
	}

	@Transient
	public ShardAssignmentDescriptor getShardAssignment() {
		return ( totalShardCount == null || assignedShardIndex == null )
				? null
				: new ShardAssignmentDescriptor( totalShardCount, assignedShardIndex );
	}
}
