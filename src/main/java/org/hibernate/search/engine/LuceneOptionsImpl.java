// $Id$
package org.hibernate.search.engine;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

import org.hibernate.search.bridge.LuceneOptions;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 * This is a package level class
 *  
 * @author Hardy Ferentschik
 */
class LuceneOptionsImpl implements LuceneOptions {
	private final Store store;
	private final Index index;
	private final TermVector termVector;
	private final Float boost;

	public LuceneOptionsImpl(Store store, Index index, TermVector termVector, Float boost) {
		this.store = store;
		this.index = index;
		this.termVector = termVector;
		this.boost = boost;
	}

	public Store getStore() {
		return store;
	}

	public Index getIndex() {
		return index;
	}

	public TermVector getTermVector() {
		return termVector;
	}

	/**
	 * @return the boost value. If <code>boost == null</code>, the default boost value
	 * 1.0 is returned.
	 */
	public Float getBoost() {
		if ( boost != null ) {
			return boost;
		} else {
			return 1.0f;
		}
	}
}