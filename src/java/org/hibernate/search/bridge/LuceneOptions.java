// $Id:$
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
	public Store store;
	public Index index;
	public TermVector termVector;
	public Float boost;

	public LuceneOptions(Store store, Index index, TermVector termVector, Float boost) {
		this.store = store;
		this.index = index;
		this.termVector = termVector;
		this.boost = boost;
	}
}