/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cache.spi;

import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.util.Version;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

/**
 * Supplies an {@link QueryCachingPolicy} for use by index search.
 *
 * @author Waldemar Kłaczyński
 */
public interface QueryCachingPolicyProvider {
	/**
	 * Returns a {@link QueryCachingPolicy} given the specified configuration properties.
	 *
	 * @param context
	 * @param properties configuration properties
	 * @param luceneVersion lucene version
	 * @return a started query cache.
	 */
	QueryCachingPolicy getQueryCachingPolicy(BackendBuildContext context, ConfigurationPropertySource properties, Version luceneVersion);
}
