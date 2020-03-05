/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.cfg;

import static java.lang.String.join;

/**
 * Configuration properties common to all Hibernate Search indexes regardless of the underlying technology.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key.
 * The exact prefix will depend on the integration, but should generally look like
 * either "{@code hibernate.search.backend.<backend name>.indexes.<index name>.}" (for per-index settings)
 * or "{@code hibernate.search.backends.<backend name>.index_defaults.}" (for default index settings).
 */
public final class IndexSettings {

	private IndexSettings() {
	}

	/**
	 * Build a concatenated version of ElasticSearchIndexSettings which contains the prefix, the backendName and the index_defaults
	 * <p>
	 * example: "{@code hibernate.search.backends.<backendName>.index_defaults.lifecycle.strategy}"
	 * </p>
	 *
	 * @param backendName Expect the backendName
	 * @param luceneOrElasticsearchBackendSettings Expect one of ElasticsearchIndexSettings constants string
	 *
	 * @return the concatenated default index settings key
	 */
	public static String indexDefaultsKey(String backendName, String luceneOrElasticsearchBackendSettings) {
		return join( ".",
				EngineSettings.BACKENDS, backendName, BackendSettings.INDEX_DEFAULTS, luceneOrElasticsearchBackendSettings
		);
	}

	/**
	 * Build a concatenated version of ElasticSearchIndexSettings or LuceneBackendSettings which contains the prefix, the backendName and the index_defaults
	 * <p>
	 * example: "{@code hibernate.search.backends.<backendName>.indexes.<indexName>.lifecycle.strategy}"
	 * </p>
	 *
	 * @param backendName Expect the backendName
	 * @param indexName Expect the specific targeted index name
	 * @param luceneOrElasticsearchBackendSettings Expect one of ElasticsearchIndexSettings or LuceneBackendSettings constants string
	 *
	 * @return the concatenated index settings key
	 */
	public static String indexKey(String backendName, String indexName, String luceneOrElasticsearchBackendSettings) {
		return join( ".",
				EngineSettings.BACKENDS, backendName, BackendSettings.INDEXES, indexName, luceneOrElasticsearchBackendSettings
		);
	}

}
