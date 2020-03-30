/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategyInitializationContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.util.common.data.impl.SimpleHashFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HashShardingStrategy implements ShardingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME = "hash";

	private static final OptionalConfigurationProperty<Integer> NUMBER_OF_SHARDS =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.NUMBER_OF_SHARDS )
					.asInteger()
					.build();

	private String[] shardIds;

	@Override
	public void initialize(ShardingStrategyInitializationContext context) {
		int numberOfShards = NUMBER_OF_SHARDS.getOrThrow(
				context.getConfigurationPropertySource(),
				key -> log.missingPropertyValueForShardingStrategy( NAME, key )
		);
		this.shardIds = new String[numberOfShards];
		Set<String> shardIdSet = new LinkedHashSet<>();
		for ( int i = 0; i < numberOfShards; i++ ) {
			String shardId = String.valueOf( i );
			shardIds[i] = shardId;
			shardIdSet.add( shardId );
		}
		context.setShardIdentifiers( shardIdSet );
	}

	@Override
	public String toShardIdentifier(String documentId, String routingKey) {
		return toShardIdentifier( routingKey == null ? documentId : routingKey );
	}

	@Override
	public Set<String> toShardIdentifiers(Set<String> routingKeys) {
		Set<String> shardIds = new LinkedHashSet<>();
		for ( String routingKey : routingKeys ) {
			shardIds.add( toShardIdentifier( routingKey ) );
		}
		return shardIds;
	}

	private String toShardIdentifier(String routingKey) {
		// Note the hash function MUST NOT CHANGE, otherwise existing indexes will no longer work correctly.
		return SimpleHashFunction.pick( shardIds, routingKey );
	}
}
