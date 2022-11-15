/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.sharding;

import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

/**
 * A basic test for hash-based sharding with explicit routing keys.
 */
@PortedFromSearch5(original = "org.hibernate.search.test.shards.ShardsTest")
class ShardingHashRoutingKeyIT extends AbstractShardingRoutingKeyIT {

	private static final int SHARD_COUNT = 3;

	public ShardingHashRoutingKeyIT() {
		super(
				tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( SHARD_COUNT ),
				// Use more routing keys than shards, so that multiple routing keys lead to the same shard, like in real life
				generateRoutingKeys( SHARD_COUNT * 4 )
		);
	}

}
