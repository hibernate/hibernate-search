/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.cache;

import org.apache.lucene.search.QueryCache;
import org.apache.lucene.search.QueryCachingPolicy;
import org.apache.lucene.util.Version;

/**
 * A context allowing the configuration of query caching in a Lucene backend.
 */
public interface QueryCachingConfigurationContext {

	/**
	 * @return The Lucene version in use in the configured backend.
	 */
	Version luceneVersion();

	/**
	 * @param cache The {@link QueryCache} to use when searching.
	 */
	void queryCache(QueryCache cache);

	/**
	 * @param policy The {@link QueryCachingPolicy} to use when searching.
	 */
	void queryCachingPolicy(QueryCachingPolicy policy);

}
