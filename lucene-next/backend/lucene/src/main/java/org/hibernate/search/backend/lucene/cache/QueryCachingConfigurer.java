/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.cache;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;

/**
 * A configurer for query caching.
 * <p>
 * Users can select a configurer through the
 * {@link LuceneBackendSettings#QUERY_CACHING_CONFIGURER configuration properties}.
 */
public interface QueryCachingConfigurer {

	/**
	 * Configures query caching as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure caching.
	 */
	void configure(QueryCachingConfigurationContext context);

}
