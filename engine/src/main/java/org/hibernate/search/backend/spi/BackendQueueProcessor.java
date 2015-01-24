/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Interface for different types of queue processors. Implementations need a no-arg constructor.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface BackendQueueProcessor {

	/**
	 * Used at startup, called once as first method.
	 *
	 * @param props all configuration properties
	 * @param context context giving access to required meta data
	 * @param indexManager the index it is related to.
	 */
	void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager);

	/**
	 * Used to shutdown and eventually release resources.
	 * No other method should be used after this one.
	 */
	void close();

	/**
	 * Applies a list of operations to the index. A single list might be processed by applying
	 * elements in parallel threads, but no work should be started on a new workList until the previous
	 * one was fully processed.
	 * Work could be applied asynchronously according to capabilities and configuration of implementor.
	 * A null parameter is not acceptable, implementations should throw an IllegalArgumentException.
	 *
	 * @param workList list of Lucene work instance which need to be applied to the index
	 * @param monitor a {@link org.hibernate.search.backend.IndexingMonitor} object.
	 */
	void applyWork(List<LuceneWork> workList, IndexingMonitor monitor);

	/**
	 * Applies a single operation on the index, and different operations can be applied in parallel,
	 * even in parallel to a workList instance being processed by {@link #applyWork(List, IndexingMonitor)}
	 *
	 * @param singleOperation single Lucene work instance to be applied to the index
	 * @param monitor a {@link org.hibernate.search.backend.IndexingMonitor} object.
	 */
	void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor);

	/**
	 * @return a Lock instance which will block index modifications when acquired
	 */
	Lock getExclusiveWriteLock();

	/**
	 * Used to notify the backend that the number or type of indexed entities being indexed
	 * in this backend changed. This could trigger some needed reconfiguration.
	 */
	void indexMappingChanged();

}
