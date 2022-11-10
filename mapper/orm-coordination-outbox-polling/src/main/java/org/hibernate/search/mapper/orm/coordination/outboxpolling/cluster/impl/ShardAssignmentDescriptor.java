/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.outboxpolling.cluster.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ShardAssignmentDescriptor {
	public static Optional<ShardAssignmentDescriptor> fromClusterMemberList(List<UUID> clusterMembersInShardOrder, UUID selfId) {
		int totalShardCount = clusterMembersInShardOrder.size();
		int assignedShard = clusterMembersInShardOrder.indexOf( selfId );
		if ( assignedShard < 0 ) {
			return Optional.empty();
		}
		return Optional.of( new ShardAssignmentDescriptor( totalShardCount, assignedShard ) );
	}

	public final int totalShardCount;
	public final int assignedShardIndex;

	public ShardAssignmentDescriptor(int totalShardCount, int assignedShardIndex) {
		this.totalShardCount = totalShardCount;
		this.assignedShardIndex = assignedShardIndex;
	}

	@Override
	public String toString() {
		return "shard " + assignedShardIndex + " (total " + totalShardCount + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ShardAssignmentDescriptor that = (ShardAssignmentDescriptor) o;
		return totalShardCount == that.totalShardCount && assignedShardIndex == that.assignedShardIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash( totalShardCount, assignedShardIndex );
	}
}
