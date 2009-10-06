/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import org.hibernate.Hibernate;
import org.hibernate.util.StringHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.impl.blackhole.BlackHoleBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.MasterJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jgroups.SlaveJGroupsBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.engine.DocumentBuilderContainedEntity;
import org.hibernate.search.util.LoggerFactory;
import org.hibernate.search.util.PluginLoader;

/**
 * Batch work until {@link #performWorks} is called.
 * The work is then executed synchronously or asynchronously.
 *
 * @author Emmanuel Bernard
 */
public class BatchedQueueingProcessor implements QueueingProcessor {

	private static final Logger log = LoggerFactory.make();

	private final boolean sync;
	private final int batchSize;
	private final ExecutorService executorService;
	private final BackendQueueProcessorFactory backendQueueProcessorFactory;
	private final SearchFactoryImplementor searchFactoryImplementor;

	public BatchedQueueingProcessor(SearchFactoryImplementor searchFactoryImplementor, Properties properties) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.sync = isConfiguredAsSync( properties );

		//default to a simple asynchronous operation
		int min = ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_THREADPOOL_SIZE, 1 );
		//no queue limit
		int queueSize = ConfigurationParseHelper.getIntValue(
				properties, Environment.WORKER_WORKQUEUE_SIZE, Integer.MAX_VALUE
		);

		batchSize = ConfigurationParseHelper.getIntValue( properties, Environment.WORKER_BATCHSIZE, 0 );

		if ( !sync ) {
			/**
			 * choose min = max with a sizable queue to be able to
			 * actually queue operations
			 * The locking mechanism preventing much of the scalability
			 * anyway, the idea is really to have a buffer
			 * If the queue limit is reached, the operation is executed by the main thread
			 */
			executorService = new ThreadPoolExecutor(
					min, min, 60, TimeUnit.SECONDS,
					new LinkedBlockingQueue<Runnable>( queueSize ),
					new ThreadPoolExecutor.CallerRunsPolicy()
			);
		}
		else {
			executorService = null;
		}
		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		if ( StringHelper.isEmpty( backend ) || "lucene".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new LuceneBackendQueueProcessorFactory();
		}
		else if ( "jms".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new JMSBackendQueueProcessorFactory();
		}
		else if ( "blackhole".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new BlackHoleBackendQueueProcessorFactory();
		}
		else if ( "jgroupsMaster".equals( backend ) ) {
				backendQueueProcessorFactory = new MasterJGroupsBackendQueueProcessorFactory();
		}
		else if ( "jgroupsSlave".equals( backend ) ) {
				backendQueueProcessorFactory = new SlaveJGroupsBackendQueueProcessorFactory();
		}
		else {
			backendQueueProcessorFactory = PluginLoader.instanceFromName( BackendQueueProcessorFactory.class,
					backend, BatchedQueueingProcessor.class, "processor" );
		}
		backendQueueProcessorFactory.initialize( properties, searchFactoryImplementor );
		searchFactoryImplementor.setBackendQueueProcessorFactory( backendQueueProcessorFactory );
	}

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

	public void prepareWorks(WorkQueue workQueue) {
		List<Work> queue = workQueue.getQueue();
		int initialSize = queue.size();
		List<LuceneWork> luceneQueue = new ArrayList<LuceneWork>( initialSize ); //TODO load factor for containedIn
		/**
		 * Collection work type are process second, so if the owner entity has already been processed for whatever reason
		 * the work will be ignored.
		 * However if the owner entity has not been processed, an "UPDATE" work is executed
		 *
		 * Processing collection works last is mandatory to avoid reindexing a object to be deleted
		 */
		processWorkByLayer( queue, initialSize, luceneQueue, Layer.FIRST );
		processWorkByLayer( queue, initialSize, luceneQueue, Layer.SECOND );
		workQueue.setSealedQueue( luceneQueue );
	}

	private <T> void processWorkByLayer(List<Work> queue, int initialSize, List<LuceneWork> luceneQueue, Layer layer) {
		for ( int i = 0; i < initialSize; i++ ) {
			@SuppressWarnings("unchecked")
			Work<T> work = queue.get( i );
			if ( work != null ) {
				if ( layer.isRightLayer( work.getType() ) ) {
					queue.set( i, null ); // help GC and avoid 2 loaded queues in memory
					addWorkToBuilderQueue( luceneQueue, work );
				}
			}
		}
	}

	private <T> void addWorkToBuilderQueue(List<LuceneWork> luceneQueue, Work<T> work) {
		@SuppressWarnings("unchecked")
		Class<T> entityClass = work.getEntityClass() != null ?
				work.getEntityClass() :
				Hibernate.getClass( work.getEntity() );
		DocumentBuilderIndexedEntity<T> entityBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityClass );
		if ( entityBuilder != null ) {
			entityBuilder.addWorkToQueue(
					entityClass, work.getEntity(), work.getId(), work.getType(), luceneQueue, searchFactoryImplementor
			);
			return;
		}

		//might be a entity contained in
		DocumentBuilderContainedEntity<T> containedInBuilder = searchFactoryImplementor.getDocumentBuilderContainedEntity( entityClass );
		if ( containedInBuilder != null ) {
			containedInBuilder.addWorkToQueue(
					entityClass, work.getEntity(), work.getId(), work.getType(), luceneQueue, searchFactoryImplementor
			);
		}
	}

	public void performWorks(WorkQueue workQueue) {
		Runnable processor = backendQueueProcessorFactory.getProcessor( workQueue.getSealedQueue() );
		if ( sync ) {
			processor.run();
		}
		else {
			executorService.execute( processor );
		}
	}

	public void cancelWorks(WorkQueue workQueue) {
		workQueue.clear();
	}

	public void close() {
		//gracefully stop
		if ( executorService != null && !executorService.isShutdown() ) {
			executorService.shutdown();
			try {
				executorService.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
			}
			catch ( InterruptedException e ) {
				log.error( "Unable to properly shut down asynchronous indexing work", e );
			}
		}
		//and stop the backend
		backendQueueProcessorFactory.close();
	}

	private static enum Layer {
		FIRST,
		SECOND;

		public boolean isRightLayer(WorkType type) {
			if ( this == FIRST && type != WorkType.COLLECTION ) {
				return true;
			}
			return this == SECOND && type == WorkType.COLLECTION;
		}
	}
	
	/**
	 * @param properties the configuration to parse
	 * @return true if the configuration uses sync indexing
	 */
	public static boolean isConfiguredAsSync(Properties properties){
		//default to sync if none defined
		return !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );
	}

}
