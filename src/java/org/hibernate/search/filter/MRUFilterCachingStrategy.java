// $Id$
package org.hibernate.search.filter;

import java.util.Properties;

import org.apache.lucene.search.Filter;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.util.SoftLimitMRUCache;

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

	public void initialize(Properties properties) {
		int size = ConfigurationParseHelper.getIntValue( properties, SIZE, DEFAULT_SIZE );
		cache = new SoftLimitMRUCache( size );
	}

	public Filter getCachedFilter(FilterKey key) {
		return (Filter) cache.get( key );
	}

	public void addCachedFilter(FilterKey key, Filter filter) {
		cache.put( key, filter );
	}
}
