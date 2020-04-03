/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cache.spi;

import org.apache.lucene.search.QueryCache;
import org.apache.lucene.util.Version;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;

/**
 * Supplies an {@link QueryCache} for use by index search.
 *
 * @author Waldemar Kłaczyński
 */
public interface QueryCacheProvider {
	/**
	 * Returns a {@link QueryCache} given the specified configuration properties.
	 *
	 * @param properties configuration properties
	 * @param luceneVersion lucene version
	 * @return a started query cache.
	 */
	QueryCache getQueryCache(ConfigurationPropertySource properties, Version luceneVersion);
}
