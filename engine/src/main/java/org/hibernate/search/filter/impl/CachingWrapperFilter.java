/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.hibernate.search.util.impl.SoftLimitMRUCache;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A slightly different version of Lucene's original <code>CachingWrapperFilter</code> which
 * uses <code>SoftReferences</code> instead of <code>WeakReferences</code> in order to cache
 * the filter <code>BitSet</code>.
 *
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @see org.apache.lucene.search.CachingWrapperFilter
 * @see <a href="https://hibernate.atlassian.net/browse/HSEARCH-174">HSEARCH-174</a>
 */
@SuppressWarnings("serial")
public class CachingWrapperFilter extends Filter {

	private static final Log log = LoggerFactory.make();

	public static final int DEFAULT_SIZE = 5;

	/**
	 * Any Filter could return null as a value representing an empty match set,
	 * we need to use NULL_OBJECT as a marker token to be able to cache this
	 * return value.
	 */
	private static final Object NULL_OBJECT = new Object();

	/**
	 * The cache using soft references in order to store the filter bit sets.
	 */
	private final SoftLimitMRUCache cache;

	private final Filter filter;

	/**
	 * Under memory pressure the JVM will release all Soft references,
	 * so pushing it too high will invalidate all eventually useful other caches.
	 */
	private static final int HARD_TO_SOFT_RATIO = 15;

	/**
	 * @param filter Filter to cache results of
	 */
	public CachingWrapperFilter(Filter filter) {
		this( filter, DEFAULT_SIZE );
	}

	/**
	 * @param filter Filter to cache results of
	 * @param size soft reference size (gets multiplied by {@link #HARD_TO_SOFT_RATIO}.
	 */
	public CachingWrapperFilter(Filter filter, int size) {
		this.filter = filter;
		final int softRefSize = size * HARD_TO_SOFT_RATIO;
		if ( log.isDebugEnabled() ) {
			log.debugf( "Initialising SoftLimitMRUCache with hard ref size of %d and a soft ref of %d", size, softRefSize );
		}
		this.cache = new SoftLimitMRUCache( size, softRefSize );
	}

	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
		final LeafReader reader = context.reader();
		Object cached = cache.get( reader );
		if ( cached != null ) {
			if ( cached == NULL_OBJECT ) {
				return null;
			}
			else {
				return (DocIdSet) cached;
			}
		}
		synchronized ( cache ) {
			cached = cache.get( reader );
			if ( cached != null ) {
				if ( cached == NULL_OBJECT ) {
					return null;
				}
				else {
					return (DocIdSet) cached;
				}
			}
			final DocIdSet docIdSet = filter.getDocIdSet( context, acceptDocs );
			if ( docIdSet == null ) {
				cache.put( reader, NULL_OBJECT );
				return null;
			}
			else {
				cache.put( reader, docIdSet );
				return docIdSet;
			}
		}
	}

	@Override
	public String toString(String field) {
		return "CachingWrapperFilter(" + filter.toString( field ) + ")";
	}

	@Override
	public boolean equals(Object o) {
		if ( !( o instanceof CachingWrapperFilter ) ) {
			return false;
		}
		return this.filter.equals( ( (CachingWrapperFilter) o ).filter );
	}

	@Override
	public int hashCode() {
		return filter.hashCode() ^ 0x1117BF25;
	}

}
