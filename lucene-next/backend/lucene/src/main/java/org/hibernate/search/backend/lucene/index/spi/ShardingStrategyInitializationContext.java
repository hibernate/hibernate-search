/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.spi;

import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface ShardingStrategyInitializationContext {

	/**
	 * @param shardIdentifiers A comprehensive set of all possible values for shard identifiers,
	 * i.e. values that can be returned by {@link ShardingStrategy#toShardIdentifier(String, String)}
	 * or {@link ShardingStrategy#toShardIdentifiers(Set)}.
	 */
	void shardIdentifiers(Set<String> shardIdentifiers);

	/**
	 * Inform Hibernate Search that sharding is disabled.
	 */
	void disableSharding();

	/**
	 * @return The name of the index in Hibernate Search.
	 */
	String indexName();

	/**
	 * @return A {@link BeanResolver}.
	 */
	BeanResolver beanResolver();

	/**
	 * @return A configuration property source, appropriately masked so that the factory
	 * doesn't need to care about Hibernate Search prefixes (hibernate.search.*, etc.). All the properties
	 * can be accessed at the root.
	 * <strong>CAUTION:</strong> the property key "type" is reserved for use by the engine.
	 */
	ConfigurationPropertySource configurationPropertySource();

}
