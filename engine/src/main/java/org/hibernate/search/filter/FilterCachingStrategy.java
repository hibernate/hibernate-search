/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

import java.util.Properties;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;

/**
 * Defines the caching filter strategy
 * implementations of getCachedFilter and addCachedFilter must be thread-safe
 *
 * @author Emmanuel Bernard
 */
public interface FilterCachingStrategy {
	/**
	 * Initialize the strategy from the properties.
	 * The Properties must not be changed.
	 *
	 * @param properties the caching strategy configuration
	 */
	void initialize(Properties properties);
	/**
	 * Retrieve the cached filter for a given key or null if not cached.
	 *
	 * @param key the filter key
	 * @return the cached filter or null if not cached
	 */
	Query getCachedFilter(FilterKey key);

	/**
	 * Propose a candidate filter for caching
	 *
	 * @param key the filter key
	 * @param filter the filter to cache
	 *
	 * @deprecated This method is not used by Hibernate Search anymore
	 * and will be removed in a future version. Please implement
	 * {@link #addCachedFilter(FilterKey, Query)} instead.
	 */
	@Deprecated
	default void addCachedFilter(FilterKey key, Filter filter) {
		/*
		 * No-op by default: we only leave this method for backward
		 * compatibility with implementations created before we introduced
		 * addCachedFilter(FilterKey, Query), but we don't want new implementations
		 * to bother with implementing it.
		 */
	}

	/**
	 * Propose a candidate filter for caching.
	 *
	 * @param key the filter key
	 * @param filter the filter to cache
	 */
	@SuppressWarnings("deprecation")
	default void addCachedFilter(FilterKey key, Query filter) {
		/*
		 * Default implementation for backward compatibility with implementations
		 * created before we introduced this method.
		 * This default implementation should be removed when we remove
		 * addCachedFilter(FilterKey, Filter).
		 */
		addCachedFilter( key, new QueryWrapperFilter( filter ) );
	}
}
