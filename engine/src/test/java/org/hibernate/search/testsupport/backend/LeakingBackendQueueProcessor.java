/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessor;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.impl.ClassLoaderHelper;

/**
 * This backend wraps the default backend to leak out the last performed list of work for testing purposes: tests can
 * inspect the queue being sent to a backend.
 * <p>
 * Depending on the given index manager, either the (local) Lucene-based backend or the Elasticsearch backend will be
 * used as delegate.
 *
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public class LeakingBackendQueueProcessor implements BackendQueueProcessor {

	private static final String ES_INDEX_MANAGER = "org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexManager";
	private static final String ES_BACKEND_QUEUE_PROCESSOR = "org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackendQueueProcessor";

	private static volatile List<LuceneWork> lastProcessedQueue = new ArrayList<LuceneWork>();

	private BackendQueueProcessor delegate;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		if ( ES_INDEX_MANAGER.equals( indexManager.getClass().getName() ) ) {
			delegate = ClassLoaderHelper.instanceFromName(
					BackendQueueProcessor.class,
					ES_BACKEND_QUEUE_PROCESSOR,
					"Elasticsearch backend",
					context.getServiceManager()
			);
		}
		else {
			delegate = new LuceneBackendQueueProcessor();
		}

		delegate.initialize( props, context, indexManager );
	}

	@Override
	public void close() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
		delegate.close();
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		delegate.applyWork( workList, monitor );
		lastProcessedQueue = workList;
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		delegate.applyStreamWork( singleOperation, monitor );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		return delegate.getExclusiveWriteLock();
	}

	@Override
	public void indexMappingChanged() {
		delegate.indexMappingChanged();
	}

	public static List<LuceneWork> getLastProcessedQueue() {
		return lastProcessedQueue;
	}

	public static void reset() {
		lastProcessedQueue = new ArrayList<LuceneWork>();
	}

	@Override
	public void closeIndexWriter() {
		delegate.closeIndexWriter();
	}
}
