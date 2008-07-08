// $Id:$
package org.hibernate.search.annotations;

/**
 * Defines the strategy for using <code>CachingWrappingFilter</code>
 * 
 * @author Hardy Ferentschik
 * @see org.hibernate.search.filter.CachingWrapperFilter
 */
public enum CachingWrapperFilter {
	/**
	 * Use a <code>CachingWrapperFilter<code> depending on the value of
	 * @see FullTextFilterDef#cache()
	 */
	AUTOMATIC,
	
	/**
	 * Wrap the filter around a <code>CachingWrappingFilter</code>.
	 */
	YES,
	
	/**
	 * Do not use a <code>CachingWrappingFilter</code>.
	 */
	NO;
}