// $Id$
package org.hibernate.search.bridge;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 * 
 * @author Hardy Ferentschik
 */
public class LuceneOptions {
	private final Store store;
	private final Index index;
	private final TermVector termVector;
	private final Float boost;

	public LuceneOptions(Store store, Index index, TermVector termVector, Float boost) {
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