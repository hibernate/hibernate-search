package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.IndexInteractionType;

/**
 * @author Sanne Grinovero
 */
public interface LuceneWorkDelegate {
	
	/**
	 * @return the IndexInteractionType needed to accomplish this work (reader or writer)
	 * 	or have a chance to express any preference for performance optimizations.
	 */
	IndexInteractionType getIndexInteractionType();

	/**
	 * Will perform work on an IndexWriter.
	 * @param work the LuceneWork to apply to the IndexWriter.
	 * @param writer the IndexWriter to use.
	 * @throws UnsupportedOperationException when the work is not compatible with an IndexWriter.
	 */
	void performWork(LuceneWork work, IndexWriter writer);
	
	/**
	 * Will perform this work on an IndexReader.
	 * @param work the LuceneWork to apply to the IndexReader.
	 * @param reader the IndexReader to use.
	 * @throws UnsupportedOperationException when the work is not compatible with an IndexReader.
	 */
	void performWork(LuceneWork work, IndexReader reader);
	
}
