/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.util.common.data.impl.HashTable;
import org.hibernate.search.util.common.data.impl.ModuloHashTable;
import org.hibernate.search.util.common.data.impl.SimpleHashFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class HashShardingStrategy implements ShardingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME = "hash";

	private static final OptionalConfigurationProperty<Integer> NUMBER_OF_SHARDS =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.NUMBER_OF_SHARDS )
					.asIntegerStrictlyPositive()
					.build();

	private HashTable<String> shardIds;

	@Override
	public void initialize(ShardingStrategyInitializationContext context) {
		int numberOfShards = NUMBER_OF_SHARDS.getOrThrow(
				context.configurationPropertySource(),
				() -> log.missingPropertyValueForShardingStrategy( NAME )
		);
		// Note the hash function / table implementations MUST NOT CHANGE,
		// otherwise existing indexes will no longer work correctly.
		this.shardIds = new ModuloHashTable<>( SimpleHashFunction.INSTANCE, numberOfShards );
		Set<String> shardIdSet = new LinkedHashSet<>();
		for ( int i = 0; i < numberOfShards; i++ ) {
			String shardId = String.valueOf( i );
			shardIds.set( i, shardId );
			shardIdSet.add( shardId );
		}
		context.shardIdentifiers( shardIdSet );
	}

	@Override
	public String toShardIdentifier(String documentId, String routingKey) {
		return toShardIdentifier( routingKey == null ? documentId : routingKey );
	}

	@Override
	public Set<String> toShardIdentifiers(Set<String> routingKeys) {
		Set<String> matchingShardIds = new LinkedHashSet<>();
		for ( String routingKey : routingKeys ) {
			matchingShardIds.add( toShardIdentifier( routingKey ) );
		}
		return matchingShardIds;
	}

	private String toShardIdentifier(String routingKey) {
		return shardIds.get( routingKey );
	}
}
