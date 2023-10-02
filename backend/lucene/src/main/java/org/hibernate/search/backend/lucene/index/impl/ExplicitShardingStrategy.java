/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategy;
import org.hibernate.search.backend.lucene.index.spi.ShardingStrategyInitializationContext;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class ExplicitShardingStrategy implements ShardingStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static final String NAME = "explicit";

	private static final OptionalConfigurationProperty<List<String>> SHARD_IDENTIFIERS =
			ConfigurationProperty.forKey( LuceneIndexSettings.ShardingRadicals.SHARD_IDENTIFIERS )
					.asString().multivalued()
					.build();

	private Set<String> shardIdSet;

	@Override
	public void initialize(ShardingStrategyInitializationContext context) {
		List<String> shardIdentifiers = SHARD_IDENTIFIERS.getOrThrow(
				context.configurationPropertySource(),
				() -> log.missingPropertyValueForShardingStrategy( NAME )
		);
		this.shardIdSet = new LinkedHashSet<>( shardIdentifiers );
		context.shardIdentifiers( shardIdSet );
	}

	@Override
	public String toShardIdentifier(String documentId, String routingKey) {
		// Ignore the document ID: the routing key must be a shard identifier
		checkShardIdentifier( routingKey );
		return routingKey;
	}

	@Override
	public Set<String> toShardIdentifiers(Set<String> routingKeys) {
		for ( String routingKey : routingKeys ) {
			checkShardIdentifier( routingKey );
		}
		return routingKeys;
	}

	private void checkShardIdentifier(String routingKey) {
		if ( !shardIdSet.contains( routingKey ) ) {
			throw log.invalidRoutingKeyForExplicitShardingStrategy( routingKey, shardIdSet );
		}
	}
}
