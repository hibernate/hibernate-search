/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * Results are not cached by Hibernate Search.
	 *
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.LeafReaderContext, org.apache.lucene.util.Bits)
	 */
	INSTANCE_ONLY,

	/**
	 * Both the filter instance and the <code>DocIdSet</code> results are cached.
	 * The filter instance is cached by Hibernate Search and reused across
	 * concurrent <code>Filter.getDocIdSet()</code> calls.
	 * <code>DocIdSet</code> results are cached per <code>IndexReader</code>.
	 *
	 * @see org.apache.lucene.search.Filter#getDocIdSet(org.apache.lucene.index.LeafReaderContext, org.apache.lucene.util.Bits)
	 */
	INSTANCE_AND_DOCIDSETRESULTS

}
