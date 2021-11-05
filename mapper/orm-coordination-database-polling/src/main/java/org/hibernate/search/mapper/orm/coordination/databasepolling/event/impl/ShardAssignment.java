/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.coordination.databasepolling.event.impl;

import java.util.Optional;

import org.hibernate.search.mapper.orm.coordination.databasepolling.cluster.impl.ShardAssignmentDescriptor;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.impl.Murmur3HashFunction;
import org.hibernate.search.util.common.data.impl.RangeCompatibleHashFunction;
import org.hibernate.search.util.common.data.impl.RangeHashTable;

final class ShardAssignment {
	// Note the hash function / table implementations MUST NOT CHANGE,
	// otherwise existing indexes will no longer work correctly.
	public static final RangeCompatibleHashFunction HASH_FUNCTION = Murmur3HashFunction.INSTANCE;

	public static ShardAssignment of(ShardAssignmentDescriptor descriptor,
			OutboxEventFinderProvider finderProvider) {
		Optional<OutboxEventPredicate> predicate;
		if ( descriptor.totalShardCount == 1 ) {
			predicate = Optional.empty();
		}
		else {
			RangeHashTable<Void> hashTable = new RangeHashTable<>( HASH_FUNCTION, descriptor.totalShardCount );
			Range<Integer> entityIdHashRange = hashTable.rangeForBucket( descriptor.assignedShardIndex );
			predicate = Optional.of( new EntityIdHashRangeOutboxEventPredicate( entityIdHashRange ) );
		}
		return new ShardAssignment( descriptor, finderProvider.create( predicate ) );
	}

	final ShardAssignmentDescriptor descriptor;
	final OutboxEventFinder eventFinder;

	// Exposed for testing purposes only
	ShardAssignment(ShardAssignmentDescriptor descriptor, OutboxEventFinder eventFinder) {
		this.descriptor = descriptor;
		this.eventFinder = eventFinder;
	}

	@Override
	public String toString() {
		return descriptor.toString();
	}

}
