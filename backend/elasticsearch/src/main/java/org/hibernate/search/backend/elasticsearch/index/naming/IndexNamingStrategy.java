/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.naming;

import org.hibernate.search.backend.elasticsearch.mapping.TypeNameMappingStrategyName;

/**
 * Defines the aliases to assign to Elasticsearch indexes,
 * as well as the structure of non-alias names.
 */
public interface IndexNamingStrategy {

	/**
	 * Generates an initial non-alias Elasticsearch name for an index.
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
