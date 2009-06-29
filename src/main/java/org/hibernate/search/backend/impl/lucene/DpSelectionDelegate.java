package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * @author Sanne Grinovero
 */
public interface DpSelectionDelegate {
	
	/**
	 * The LuceneWork must be applied to different indexes.
	 * @param work the work to split.
	 * @param queues the target queue to add work to.
	 * @param shardingStrategy the Sharding strategy is usually needed to identify affected Directories. 
	 * @throws InterruptedException 
	 */
	void addAsPayLoadsToQueue(LuceneWork work,
			IndexShardingStrategy shardingStrategy, PerDirectoryWorkProcessor queues) throws InterruptedException;

}
