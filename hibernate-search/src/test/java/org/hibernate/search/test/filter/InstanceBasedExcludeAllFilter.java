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
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSet;
import org.hornetq.utils.ConcurrentHashSet;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class InstanceBasedExcludeAllFilter extends Filter implements Serializable {
	
	// ugly but useful for test purposes
	private final Set<IndexReader> invokedOnReaders = new ConcurrentHashSet<IndexReader>();

	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		ExcludeAllFilter.verifyItsAReadOnlySegmentReader( reader );
		if ( invokedOnReaders.contains( reader ) ) {
			throw new IllegalStateException( "Called twice" );
		}
		invokedOnReaders.add( reader );
		return DocIdSet.EMPTY_DOCIDSET;
	}
	
}
