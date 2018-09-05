/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter;

import java.util.Properties;

import org.hibernate.search.exception.SearchException;

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
	 * <p>
	 * <strong>Caution:</strong> this method has a default implementation for technical reasons,
	 * but it <em>must</em> be implemented.
	 *
	 * @param key the filter key
	 * @return the cached filter or null if not cached
	 */
	default Query getCachedFilter(FilterKey key) {
		return getCachedFilter$$bridge$$FilterReturnType( key );
	}

	/**
	 * <strong>Not part of the API!</strong> Do not use or implement this.
	 * @deprecated This method is only here so that Hibernate Search build tools can generate bytecode
	 * allowing to preserve binary compatibility with applications written for Hibernate Search 5.5.
	 * It will be removed in a future version. Please implement {@link #getCachedFilter(FilterKey)} instead.
	 * @param key the filter key
	 * @return the cached filter or null if not cached
	 */
	@Deprecated
	default Filter getCachedFilter$$bridge$$FilterReturnType(FilterKey key) {
		throw new SearchException(
				"Custom filter caching strategy " + getClass().getName()
				+ " does not implement getCachedFilter(FilterKey) as required."
		);
	}

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
