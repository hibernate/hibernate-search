/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl;

import java.util.List;
import java.util.Objects;

public class ClusterDescriptor {
	public final List<Long> memberIdsInShardOrder;

	public ClusterDescriptor(List<Long> memberIdsInShardOrder) {
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
