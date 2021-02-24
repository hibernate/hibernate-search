/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
