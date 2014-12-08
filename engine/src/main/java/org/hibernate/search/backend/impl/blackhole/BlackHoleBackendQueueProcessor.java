/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.blackhole;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This backend does not do anything: the Documents are not
 * sent to any index but are discarded.
 * Useful to identify the bottleneck in indexing performance problems,
 * fully disabling the backend system but still building the Documents
 * needed to update an index (loading data from DB).
 *
 * @author Sanne Grinovero
 */
public class BlackHoleBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private final ReentrantLock backendLock = new ReentrantLock();

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		// no-op
		log.initializedBlackholeBackend();
	}

	@Override
	public void close() {
		// no-op
		log.closedBlackholeBackend();
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		// no-op
		log.debug( "Discarding a list of LuceneWork" );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		// no-op
		log.debug( "Discarding a single LuceneWork" );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		return backendLock;
	}

	@Override
	public void indexMappingChanged() {
		// no-op
		log.debug( "BlackHoleBackend reconfigured" );
	}

}
