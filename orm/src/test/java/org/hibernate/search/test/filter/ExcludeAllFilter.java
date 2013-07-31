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
package org.hibernate.search.test.filter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.index.IndexReader;
import org.hibernate.search.SearchException;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class ExcludeAllFilter extends Filter implements Serializable {

	// ugly but useful for test purposes
	private static final Map<IndexReader,IndexReader> invokedOnReaders = new ConcurrentHashMap<IndexReader,IndexReader>();

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		verifyItsAReadOnlySegmentReader( reader );
		final IndexReader previousValue = invokedOnReaders.put( reader, reader );
		if ( previousValue != null ) {
			throw new IllegalStateException( "Called twice" );
		}
		return DocIdSet.EMPTY_DOCIDSET;
	}

	public static void verifyItsAReadOnlySegmentReader(IndexReader reader) {
		String implementationName = reader.getClass().getName();
		if ( ! "org.apache.lucene.index.ReadOnlySegmentReader".equals( implementationName ) ) {
			throw new SearchException( "test failed: we should receive subreaders" );
		}
	}

}
