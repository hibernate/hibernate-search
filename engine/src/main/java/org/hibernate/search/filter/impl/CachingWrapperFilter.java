/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.filter.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;

import org.hibernate.search.util.impl.SoftLimitMRUCache;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A slightly different version of Lucene's original <code>CachingWrapperFilter</code> which
 * uses <code>SoftReferences</code> instead of <code>WeakReferences</code> in order to cache
 * the filter <code>BitSet</code>.
 *
 * @author Hardy Ferentschik
 * @see org.apache.lucene.search.CachingWrapperFilter
 * @see <a href="http://opensource.atlassian.com/projects/hibernate/browse/HSEARCH-174">HSEARCH-174</a>
 */
@SuppressWarnings("serial")
public class CachingWrapperFilter extends Filter {

	private static final Log log = LoggerFactory.make();

	public static final int DEFAULT_SIZE = 5;

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
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		DocIdSet cached = (DocIdSet) cache.get( reader );
		if ( cached != null ) {
			return cached;
		}
		synchronized ( cache ) {
			cached = (DocIdSet) cache.get( reader );
			if ( cached != null ) {
				return cached;
			}
			final DocIdSet docIdSet = filter.getDocIdSet( reader );
			cache.put( reader, docIdSet );
			return docIdSet;
		}
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "(" + filter + ")";
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
