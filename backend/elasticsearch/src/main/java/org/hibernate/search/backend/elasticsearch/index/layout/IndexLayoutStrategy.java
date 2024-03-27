/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index.layout;

import org.hibernate.search.backend.elasticsearch.mapping.TypeNameMappingStrategyName;

/**
 * Defines the layout of indexes on the Elasticsearch side:
 * the name of aliases to assign to Elasticsearch indexes,
 * as well as the structure of non-alias names.
 */
public interface IndexLayoutStrategy {

	/**
	 * Generates an initial non-alias Elasticsearch name for an index.
	 * <p>
	 * When {@link #createReadAlias(String)} or {@link #createWriteAlias(String)} returns {@code null},
	 * this must consistently return the same value for each index,
	 * even across multiple executions of the application.
	 * Otherwise, the only requirement is that returned names are unique.
	 *
	 * @param hibernateSearchIndexName The Hibernate Search name of an index.
	 * @return The non-alias Elasticsearch name for this index.
	 */
	String createInitialElasticsearchIndexName(String hibernateSearchIndexName);

	/**
	 * Generates the write alias for an index.
	 * <p>
	 * This alias will be used when indexing documents, purging the index, ...
	 * <p>
	 * This must consistently return the same value for each index,
	 * even across multiple executions of the application.
	 * <p>
	 * If you do not want to use aliases for write operations, return {@code null}:
	 * the non-alias name returned by {@link #createInitialElasticsearchIndexName(String)}
	 * will be used instead.
	 *
	 * @param hibernateSearchIndexName The Hibernate Search name of an index.
	 * @return The write alias for this index.
	 */
	String createWriteAlias(String hibernateSearchIndexName);

	/**
	 * Generates the read alias for an index.
	 * <p>
	 * This alias will be used when executing search queries.
	 * <p>
	 * This must consistently return the same value for each index,
	 * even across multiple executions of the application.
	 * <p>
	 * If you do not want to use aliases for read operations, return {@code null}:
	 * the non-alias name returned by {@link #createInitialElasticsearchIndexName(String)}
	 * will be used instead.
	 *
	 * @param hibernateSearchIndexName The Hibernate Search name of an index.
	 * @return The read alias for this index.
	 */
	String createReadAlias(String hibernateSearchIndexName);

	/**
	 * Extracts a unique key from a Hibernate Search index name.
	 * <p>
	 * Optional operation: this method only has to be implemented
	 * when using the {@link TypeNameMappingStrategyName#INDEX_NAME index-name} type-name mapping strategy.
	 * <p>
	 * This method will be called once per index on bootstrap.
	 * <p>
	 * The returned key must be consistent with the key returned by {@link #extractUniqueKeyFromElasticsearchIndexName(String)}.
	 *
	 * @param hibernateSearchIndexName The Hibernate Search name of an index.
	 * @return The unique key assigned to that index.
	 */
	default String extractUniqueKeyFromHibernateSearchIndexName(String hibernateSearchIndexName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Extracts a unique key from a (non-alias) Elasticsearch index name.
	 * <p>
	 * Optional operation: this method only has to be implemented
	 * when using the {@link TypeNameMappingStrategyName#INDEX_NAME index-name} type-name mapping strategy.
	 * <p>
	 * This method will be called once per index on bootstrap.
	 * <p>
	 * The returned key must be consistent with the key returned by {@link #extractUniqueKeyFromHibernateSearchIndexName(String)}.
	 *
	 * @param elasticsearchIndexName A primary index name extracted from an Elasticsearch response.
	 * @return The unique key assigned to that index.
	 */
	default String extractUniqueKeyFromElasticsearchIndexName(String elasticsearchIndexName) {
		throw new UnsupportedOperationException();
	}


}
