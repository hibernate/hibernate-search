// $Id$
package org.hibernate.search.annotations;

/**
 * Defines the strategy for caching the <code>BitSet</code> returned by a defined filter.
 * 
 * @author Hardy Ferentschik
 * @see org.hibernate.search.filter.CachingWrapperFilter
 */
public enum CacheBitResults {
	/**
	 * Caching is dependent on the value of the <code>cache</code>
	 * parameter of the filter definition. If <code>cache == true</code> a wrapping filter will 
	 * be used, otherwise not.
	 * @see FullTextFilterDef#cache()
	 */
	AUTOMATIC,
	
	/**
	 * The filters <code>BitSet</code> will be cached.
	 */
	YES,
	
	/**
	 * No caching of the filter's <code>BitSet</code>.
	 */
	NO;
}