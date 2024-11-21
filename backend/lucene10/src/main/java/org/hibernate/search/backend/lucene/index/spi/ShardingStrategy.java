/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.spi;

import java.util.Set;

/**
 * A strategy for translating routing keys into actual shard identifiers
 * <p>
 * With the exception of the {@link ShardingStrategy#initialize(ShardingStrategyInitializationContext)}
 * method which is invoked only once at startup,
 * all methods could be invoked in parallel by independent threads.
 * Implementations must thus be thread-safe.
 * <p>
 * Ported from Search 5: {@code org.hibernate.search.store.ShardIdentifierProvider}.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 */
public interface ShardingStrategy {

	/**
	 * Initializes the sharding strategy.
	 *
	 * @param context The initialization context, giving access to configuration and environment.
	 * The sharding strategy is expected to call {@link ShardingStrategyInitializationContext#shardIdentifiers(Set)}.
	 */
	void initialize(ShardingStrategyInitializationContext context);

	/**
	 * Returns the shard identifier corresponding to the given document ID and routing key.
	 * <p>
	 * Called in particular when indexing documents.
	 *
	 * @param documentId A document identifier. Never {@code null}.
	 * @param routingKey A routing key. Never {@code null}.
	 * @return A shard identifiers corresponding to the given document ID and routing key. Never {@code null}.
	 */
	String toShardIdentifier(String documentId, String routingKey);

	/**
	 * Returns all the shard identifiers that can be assigned to the given routing keys
	 * by {@link #toShardIdentifier(String, String)}.
	 * <p>
	 * Called in particular when searching.
	 *
	 * @param routingKeys A set of routing keys. Never {@code null}, never empty.
	 * @return All the shard identifiers that can be assigned to the given routing keys.
	 */
	Set<String> toShardIdentifiers(Set<String> routingKeys);

}
