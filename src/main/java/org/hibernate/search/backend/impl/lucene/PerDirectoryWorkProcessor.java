//$Id$
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.DirectoryProvider;

/**
 * Interface to implement visitor pattern in combination
 * with DpSelectionVisitor and DpSelectionDelegate to
 * send LuceneWork to the appropriate queues, as defined
 * by an IndexShardingStrategy.
 * 
 * @author Sanne Grinovero
 */
public interface PerDirectoryWorkProcessor {
	
	public void addWorkToDpProcessor(DirectoryProvider<?> dp, LuceneWork work) throws InterruptedException;

}
