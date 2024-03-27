/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

public class AgentPayload {
	public final ShardAssignmentDescriptor staticShardAssignment;
	public final ClusterDescriptor cluster;

	public AgentPayload(ShardAssignmentDescriptor staticShardAssignment, ClusterDescriptor cluster) {
		this.staticShardAssignment = staticShardAssignment;
		this.cluster = cluster;
	}
}
