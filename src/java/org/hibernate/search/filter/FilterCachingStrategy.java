// $Id$
package org.hibernate.search.filter;

import java.util.Properties;

import org.apache.lucene.search.Filter;

/**
 * Defines the caching filter strategy
 * implementations of getCachedFilter and addCachedFilter must be thread-safe 
 *
 * @author Emmanuel Bernard
 */
public interface FilterCachingStrategy {
	/**
	 * initialize the strategy from the properties
	 * The Properties must not be changed
	 */
	void initialize(Properties properties);
	/**
	 * Retrieve the cached filter for a given key or null if not cached
	 */
	Filter getCachedFilter(FilterKey key);

	/**
	 * Propose a candidate filter for caching
	 */
	void addCachedFilter(FilterKey key, Filter filter);
}
