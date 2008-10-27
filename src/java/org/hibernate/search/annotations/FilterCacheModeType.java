package org.hibernate.search.annotations;

/**
 * Cache mode strategy for <code>FullTextFilterDef</code>s.
 *
 * @see FullTextFilterDef
 * @author Emmanuel Bernard
 */
public enum FilterCacheModeType {
	/**
	 * No filter instance and no result is cached by Hibernate Search.
	 * For every filter call, a new filter instance is created.
	 */
	NONE,

	/**
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.bits()</code> calls.
	 * Results are not cached by Hibernate Search.
	 *
	 * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader)

	 */
	INSTANCE_ONLY,

	/**
	 * Both the filter instance and the <code>BitSet</code> results are cached.
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.bits()</code> calls.
	 * <code>BitSet</code> results are cached per <code>IndexReader</code>.
	 *
	 * @see org.apache.lucene.search.Filter#bits(org.apache.lucene.index.IndexReader) 
	 */
	INSTANCE_AND_BITSETRESULTS

}
