package org.hibernate.search.annotations;

/**
 * Cache mode strategy for Full Text filters
 *
 * @author Emmanuel Bernard
 */
public enum FilterCacheModeType {
	/**
	 * NO filter instance and no result is cached by Hibernate Search
	 * For every filter call, a new filter instance is created
	 */
	NO,

	/**
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent filter.bits() calls
	 * Results are not cache by Hibernate Search
	 */
	INSTANCE_ONLY,

	/**
	 * Both the filter instance and the BitSet results are cached.
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent filter.bits() calls
	 * BitSet Results are cached per IndexReader 
	 */
	INSTANCE_AND_RESULTS

}
