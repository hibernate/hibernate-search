//$Id$
package org.hibernate.search.backend.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hibernate.Hibernate;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkType;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.backend.impl.jms.JMSBackendQueueProcessorFactory;
import org.hibernate.search.backend.impl.lucene.LuceneBackendQueueProcessorFactory;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Batch work until #performWorks is called.
 * The work is then executed synchronously or asynchronously
 *
 * @author Emmanuel Bernard
 */
public class BatchedQueueingProcessor implements QueueingProcessor {
	private boolean sync;
	private int batchSize;
	private ExecutorService executorService;
	private BackendQueueProcessorFactory backendQueueProcessorFactory;
	private SearchFactoryImplementor searchFactoryImplementor;

	public BatchedQueueingProcessor(SearchFactoryImplementor searchFactoryImplementor,
									Properties properties) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		//default to sync if none defined
		this.sync = !"async".equalsIgnoreCase( properties.getProperty( Environment.WORKER_EXECUTION ) );

		//default to a simple asynchronous operation
		int min = Integer.parseInt(
				properties.getProperty( Environment.WORKER_THREADPOOL_SIZE, "1" ).trim()
		);
		//no queue limit
		int queueSize = Integer.parseInt(
				properties.getProperty( Environment.WORKER_WORKQUEUE_SIZE, Integer.toString( Integer.MAX_VALUE ) ).trim()
		);
		batchSize = Integer.parseInt(
				properties.getProperty( Environment.WORKER_BATCHSIZE, "0" ).trim()
		);
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
					new LinkedBlockingQueue<Runnable>(queueSize),
					new ThreadPoolExecutor.CallerRunsPolicy()
			);
		}
		String backend = properties.getProperty( Environment.WORKER_BACKEND );
		if ( StringHelper.isEmpty( backend ) || "lucene".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new LuceneBackendQueueProcessorFactory();
		}
		else if ( "jms".equalsIgnoreCase( backend ) ) {
			backendQueueProcessorFactory = new JMSBackendQueueProcessorFactory();
		}
		else {
			try {
				Class processorFactoryClass = ReflectHelper.classForName( backend, BatchedQueueingProcessor.class );
				backendQueueProcessorFactory = (BackendQueueProcessorFactory) processorFactoryClass.newInstance();
			}
			catch (ClassNotFoundException e) {
				throw new SearchException( "Unable to find processor class: " + backend, e );
			}
			catch (IllegalAccessException e) {
				throw new SearchException( "Unable to instanciate processor class: " + backend, e );
			}
			catch (InstantiationException e) {
				throw new SearchException( "Unable to instanciate processor class: " + backend, e );
			}
		}
		backendQueueProcessorFactory.initialize( properties, searchFactoryImplementor );
		searchFactoryImplementor.setBackendQueueProcessorFactory( backendQueueProcessorFactory );
	}

	public void add(Work work, WorkQueue workQueue) {
		//don't check for builder it's done in prepareWork
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
		for ( int i = 0 ; i < initialSize ; i++ ) {
			Work work = queue.get( i );
			queue.set( i, null ); // help GC and avoid 2 loaded queues in memory
			Class entityClass = work.getEntityClass() != null ?
						work.getEntityClass() :
						Hibernate.getClass( work.getEntity() );
			DocumentBuilder<Object> builder = searchFactoryImplementor.getDocumentBuilders().get( entityClass );
			if ( builder == null ) return; //or exception?
			builder.addWorkToQueue(entityClass, work.getEntity(), work.getId(), work.getType(), luceneQueue, searchFactoryImplementor );
		}
		workQueue.setSealedQueue( luceneQueue );
	}

	//TODO implements parallel batchWorkers (one per Directory)
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

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		//gracefully stop
		//TODO move to the SF close lifecycle
		if ( executorService != null && !executorService.isShutdown() ) {
			executorService.shutdown();
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS );
		}
	}

}
