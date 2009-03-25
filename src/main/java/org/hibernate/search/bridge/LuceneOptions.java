package org.hibernate.search.bridge;

import org.apache.lucene.document.Field;

/**
 * A wrapper class for Lucene parameters needed for indexing.
 * 
 * @author Emmanuel Bernard
 */
public interface LuceneOptions {
	Field.Store getStore();

	Field.Index getIndex();

	Field.TermVector getTermVector();

	/**
	 * @return the boost value. If <code>boost == null</code>, the default boost value
	 * 1.0 is returned.
	 */
	Float getBoost();
}
