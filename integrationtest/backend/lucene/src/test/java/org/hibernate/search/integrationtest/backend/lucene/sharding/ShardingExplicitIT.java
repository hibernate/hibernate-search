/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.sharding;

import java.util.Set;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.util.LuceneTckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.sharding.AbstractShardingRoutingKeyIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.util.common.impl.CollectionHelper;

/**
 * A basic test for explicit sharding with explicit routing keys.
 */
public class ShardingExplicitIT extends AbstractShardingRoutingKeyIT {

	protected static TckBackendSetupStrategy<?> explicitShardingBackendSetupStrategy(Set<String> shardIds) {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( LuceneIndexSettings.SHARDING_STRATEGY, "explicit" )
				.setProperty( LuceneIndexSettings.SHARDING_SHARD_IDENTIFIERS, String.join( ",", shardIds ) );
	}

	private static final String SHARD_ID_1 = "first";
	private static final String SHARD_ID_2 = "second";
	private static final String SHARD_ID_3 = "third";
	private static final Set<String> SHARD_IDS = CollectionHelper.asImmutableSet(
			SHARD_ID_1, SHARD_ID_2, SHARD_ID_3
	);

	public ShardingExplicitIT() {
		super( ignored -> explicitShardingBackendSetupStrategy( SHARD_IDS ), SHARD_IDS );
	}

}
