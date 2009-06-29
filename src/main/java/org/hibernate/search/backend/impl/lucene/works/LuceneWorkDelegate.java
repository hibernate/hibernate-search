package org.hibernate.search.backend.impl.lucene.works;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.batchindexing.IndexerProgressMonitor;

/**
 * @author Sanne Grinovero
 */
public interface LuceneWorkDelegate {
	
	/**
	 * Will perform work on an IndexWriter.
	 * @param work the LuceneWork to apply to the IndexWriter.
	 * @param writer the IndexWriter to use.
	 * @throws UnsupportedOperationException when the work is not compatible with an IndexWriter.
	 */
	void performWork(LuceneWork work, IndexWriter writer);
	
	/**
	 * Used for stats and performance counters, use the monitor
	 * to keep track of activity done on the index.
	 * @param work the work which was done.
	 * @param monitor the monitor tracking activity.
	 */
	void logWorkDone(LuceneWork work, IndexerProgressMonitor monitor);
	
}
