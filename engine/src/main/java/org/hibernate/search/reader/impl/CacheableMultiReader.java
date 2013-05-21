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
package org.hibernate.search.reader.impl;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.hibernate.search.indexes.spi.ReaderProvider;

/**
 * MultiReader ensuring equals returns true if the underlying readers are the same (and in the same order)
 * Especially useful when using {@link org.apache.lucene.search.CachingWrapperFilter}
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CacheableMultiReader extends MultiReader {

	// This is package private as the intention of the Lucene team seems to be to not
	// expose this publically (it's a protected member in Lucene 2.3)
	final IndexReader[] subReaders;
	final ReaderProvider[] managers;

	public CacheableMultiReader(IndexReader[] subReaders, ReaderProvider[] managers) {
		// If this flag isn't set to true, the MultiReader will increase the usage counter!
		super( subReaders, true );
		this.subReaders = subReaders;
		this.managers = managers;
	}

	@Override
	public boolean equals(Object obj) {
		// Equality only checks for subReaders as an equal sub-IndexReader is certainly coming from the same ReaderProvider.
		if ( this == obj ) {
			return true;
		}
		if ( !( obj instanceof CacheableMultiReader ) ) {
			return false;
		}
		CacheableMultiReader that = (CacheableMultiReader) obj;
		int length = this.subReaders.length;
		if ( length != that.subReaders.length ) {
			return false;
		}
		for ( int index = 0; index < length; index++ ) {
			if ( !this.subReaders[index].equals( that.subReaders[index] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;
		for ( Object reader : this.subReaders ) {
			result = 31 * result + reader.hashCode();
		}
		return result;
	}

}
