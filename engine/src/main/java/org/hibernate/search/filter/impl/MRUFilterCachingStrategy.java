/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.util.Properties;

import org.apache.lucene.search.Filter;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.impl.SoftLimitMRUCache;

/**
 * Keep the most recently used Filters in the cache
 * The cache is at least as big as <code>hibernate.search.filter.cache_strategy.size</code>
 * Above this limit, Filters are kept as soft references
 *
 * @author Emmanuel Bernard
 */
public class MRUFilterCachingStrategy implements FilterCachingStrategy {
	private static final int DEFAULT_SIZE = 128;
	private SoftLimitMRUCache cache;
	private static final String SIZE = Environment.FILTER_CACHING_STRATEGY + ".size";

	/**
	 * Under memory pressure the JVM will release all Soft references,
	 * so pushing it too high will invalidate all eventually useful other caches.
	 */
	private static final int HARD_TO_SOFT_RATIO = 15;

	@Override
	public void initialize(Properties properties) {
		int size = ConfigurationParseHelper.getIntValue( properties, SIZE, DEFAULT_SIZE );
		cache = new SoftLimitMRUCache( size, size * HARD_TO_SOFT_RATIO );
	}

	@Override
	public Filter getCachedFilter(FilterKey key) {
		return (Filter) cache.get( key );
	}

	@Override
	public void addCachedFilter(FilterKey key, Filter filter) {
		cache.put( key, filter );
	}
}
