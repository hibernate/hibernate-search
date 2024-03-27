/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.cfg;

import static java.lang.String.join;

/**
 * Configuration properties common to all Hibernate Search indexes regardless of the underlying technology.
 * <p>
 * Constants in this class are to be appended to a prefix to form a property key.
 * The exact prefix will be either
 * "{@code hibernate.search.backend.indexes.<index-name>.}" (for per-index settings)
 * or "{@code hibernate.search.backend.}" (for default index settings).
 */
public final class IndexSettings {

	private IndexSettings() {
	}

	/**
	 * Builds a configuration property key for the index of the given backend, with the given radical.
	 * <p>
	 * See the javadoc of your backend for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backend.indexes.myIndex.indexing.queue_size}"
	 *
	 * @param indexName The name of the index to configure.
	 * @param radical The radical of the configuration property (see constants in
	 * {@code ElasticsearchIndexSettings}, {@code LuceneIndexSettings}, etc.)
	 * @return the concatenated index settings key
	 */
	public static String indexKey(String indexName, String radical) {
		return join( ".", EngineSettings.BACKEND, BackendSettings.INDEXES, indexName, radical );
	}

	/**
	 * Builds a configuration property key for the index of the given backend, with the given radical.
	 * <p>
	 * See the javadoc of your backend for available radicals.
	 * </p>
	 * Example result: "{@code hibernate.search.backends.<backendName>.indexes.<indexName>.indexing.queue_size}"
	 *
	 * @param backendName The name of the backend in which the index to configure is located.
	 * @param indexName The name of the index to configure.
	 * @param radical The radical of the configuration property (see constants in
	 * {@code ElasticsearchIndexSettings}, {@code LuceneIndexSettings}, etc.)
	 * @return the concatenated index settings key
	 */
	public static String indexKey(String backendName, String indexName, String radical) {
		if ( backendName == null ) {
			return indexKey( indexName, radical );
		}
		return join( ".", EngineSettings.BACKENDS, backendName, BackendSettings.INDEXES, indexName, radical );
	}

}
