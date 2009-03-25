// $Id$
package org.hibernate.search.reader;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;

/**
 * MultiReader ensuring equals returns true if the underlying readers are the same (and in the same order)
 * Especially useful when using {@link org.apache.lucene.search.CachingWrapperFilter}
 *
 * @author Emmanuel Bernard
 */
public class CacheableMultiReader extends MultiReader {

	// This is package private as the intention of the Lucene team seems to be to not 
	// expose this publically (it's a protected member in Lucene 2.3)
	final IndexReader[] subReaders;

	public CacheableMultiReader(IndexReader[] subReaders) throws IOException {
		super( subReaders );
		this.subReaders = subReaders;
	}

	/**
	 * only available since 2.3
	 */
	/*
	public CacheableMultiReader(IndexReader[] subReaders, boolean closeSubReaders) {
		super( subReaders, closeSubReaders );
		this.subReaders = subReaders;
	}
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) return true;
		if ( !( obj instanceof CacheableMultiReader ) ) return false;
		CacheableMultiReader that = (CacheableMultiReader) obj;
		int length = this.subReaders.length;
		if ( length != that.subReaders.length ) return false;
		for (int index = 0; index < length; index++) {
			if ( !this.subReaders[index].equals( that.subReaders[index] ) ) return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (Object reader : this.subReaders) {
			result = 31 * result + reader.hashCode();
		}
		return result;
	}
}
