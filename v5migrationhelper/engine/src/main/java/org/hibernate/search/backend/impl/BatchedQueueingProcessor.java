/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.List;
import java.util.Properties;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
/**
 * Batch work until {@link #performWorks} is called.
 * The work is then executed synchronously or asynchronously.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class BatchedQueueingProcessor implements QueueingProcessor {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final int batchSize;
	private final TransactionalOperationDispatcher operationDispatcher;

	public BatchedQueueingProcessor(IndexedTypeMap<EntityIndexBinding> documentBuildersIndexedEntities, Properties properties, IndexManagerHolder indexManagerHolder) {
		batchSize = ConfigurationParseHelper.getIntValue( properties, Environment.QUEUEINGPROCESSOR_BATCHSIZE, 0 );
		this.operationDispatcher = new TransactionalOperationDispatcher( indexManagerHolder, documentBuildersIndexedEntities );
	}

	@Override
	public void add(Work work, WorkQueue workQueue) {
		//don't check for builder it's done in prepareWork
		//FIXME WorkType.COLLECTION does not play well with batchSize
		workQueue.add( work );
		if ( batchSize > 0 && workQueue.size() >= batchSize ) {
			WorkQueue subQueue = workQueue.splitQueue();
			prepareWorks( subQueue );
			performWorks( subQueue );
		}
	}

	@Override
	public void prepareWorks(WorkQueue workQueue) {
		workQueue.prepareWorkPlan();
	}

	@Override
	public void performWorks(WorkQueue workQueue) {
		List<LuceneWork> sealedQueue = workQueue.getSealedQueue();
		if ( log.isTraceEnabled() ) {
			StringBuilder sb = new StringBuilder( "Lucene WorkQueue to send to backends:[ \n\t" );
			for ( LuceneWork lw : sealedQueue ) {
				sb.append( lw.toString() );
				sb.append( "\n\t" );
			}
			if ( sealedQueue.size() > 0 ) {
				sb.deleteCharAt( sb.length() - 1 );
			}
			sb.append( "]" );
			log.trace( sb.toString() );
		}
		operationDispatcher.dispatch( sealedQueue, null );
	}

	@Override
	public void cancelWorks(WorkQueue workQueue) {
		workQueue.clear();
	}

}
