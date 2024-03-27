/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.cluster.impl;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ClusterDescriptor {
	public final List<UUID> memberIdsInShardOrder;

	public ClusterDescriptor(List<UUID> memberIdsInShardOrder) {
		this.memberIdsInShardOrder = memberIdsInShardOrder;
	}

	@Override
	public String toString() {
		return memberIdsInShardOrder.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ClusterDescriptor that = (ClusterDescriptor) o;
		return Objects.equals( memberIdsInShardOrder, that.memberIdsInShardOrder );
	}

	@Override
	public int hashCode() {
		return Objects.hash( memberIdsInShardOrder );
	}
}
