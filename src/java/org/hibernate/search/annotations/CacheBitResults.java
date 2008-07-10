// $Id:$
package org.hibernate.search.annotations;

/**
 * Defines the strategy for using <code>CachingWrappingFilter</code>
 * 
 * @author Hardy Ferentschik
 * @see org.hibernate.search.filter.CachingWrapperFilter
 */
public enum CacheBitResults {
	/**
	 * Use a <code>CachingWrapperFilter<code> depending on the value of the <code>cache</code>
	 * parameter of the filter definition. If <code>cache == true</code> a wrapping filter will 
	 * be used, otherwise not.
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