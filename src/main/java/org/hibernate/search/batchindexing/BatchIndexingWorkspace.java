package org.hibernate.search.batchindexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 * 
 * @author Sanne Grinovero
 */
public class BatchIndexingWorkspace implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final SearchFactoryImplementor searchFactory;
	private final SessionFactory sessionFactory;
	
	//following order shows the 4 stages of an entity flowing to the index:
	private final ThreadPoolExecutor 		execIdentifiersLoader;
	private final ProducerConsumerQueue 	fromIdentifierListToEntities;
	private final ThreadPoolExecutor 		execFirstLoader;
	private final ProducerConsumerQueue 	fromEntityToAddwork;
	private final ThreadPoolExecutor		execDocBuilding;
	private final ProducerConsumerQueue		fromAddworkToIndex;
	private final ThreadPoolExecutor 		execWriteIndex;
	
	private final int objectLoadingThreadNum;
	private final int luceneworkerBuildingThreadNum;
	private final int indexWritingThreadNum;
	private final Class<?> indexedType;
	
	// status control
	private final AtomicBoolean started = new AtomicBoolean( false );
	private final CountDownLatch endWritersSignal; //released when we stop adding Documents to Index 
	private final CountDownLatch endAllSignal; //released when we release all locks and IndexWriter
	
	// progress monitor
	private final IndexerProgressMonitor monitor;

	// loading options
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;

	private final boolean purgeAtStart;
	private final boolean optimizeAfterPurge;
	private final boolean optimizeAtEnd;

	public BatchIndexingWorkspace(SearchFactoryImplementor searchFactoryImplementor, SessionFactory sessionFactory,
			Class<?> entityType,
			int objectLoadingThreads, int collectionLoadingThreads, int writerThreads,
			CacheMode cacheMode, int objectLoadingBatchSize,
			boolean optimizeAtEnd, boolean purgeAtStart, boolean optimizeAfterPurge, CountDownLatch endAllSignal,
			IndexerProgressMonitor monitor) {
		
		this.indexedType = entityType;
		this.searchFactory = searchFactoryImplementor;
		this.sessionFactory = sessionFactory;
		
		//thread pool sizing:
		this.objectLoadingThreadNum = objectLoadingThreads;
		this.luceneworkerBuildingThreadNum = collectionLoadingThreads;//collections are loaded as needed by building the document
//		this.indexWritingThreadNum = writerThreads; //FIXME enable this line and remove the next line after solving HSEARCH-367
		this.indexWritingThreadNum = 1;
		
		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;
		
		//executors: (quite expensive constructor)
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		this.execIdentifiersLoader = Executors.newFixedThreadPool( 1, "identifierloader" );
		this.execFirstLoader = Executors.newFixedThreadPool( objectLoadingThreadNum, "entityloader" );
		this.execDocBuilding = Executors.newFixedThreadPool( luceneworkerBuildingThreadNum, "collectionsloader" );
		this.execWriteIndex = Executors.newFixedThreadPool( indexWritingThreadNum, "analyzers" );
		
		//pipelining queues:
		this.fromIdentifierListToEntities = new ProducerConsumerQueue( 1 );
		this.fromEntityToAddwork = new ProducerConsumerQueue( objectLoadingThreadNum );
		this.fromAddworkToIndex = new ProducerConsumerQueue( luceneworkerBuildingThreadNum );
		
		//end signal shared with other instances:
		this.endAllSignal = endAllSignal;
		this.endWritersSignal = new CountDownLatch( indexWritingThreadNum );
		
		//behaviour options:
		this.optimizeAtEnd = optimizeAtEnd;
		this.optimizeAfterPurge = optimizeAfterPurge;
		this.purgeAtStart = purgeAtStart;
		
		this.monitor = monitor;
	}

	public void run() {
		if ( ! started.compareAndSet( false, true ) )
			throw new IllegalStateException( "BatchIndexingWorkspace can be started only once." );
		boolean interrupted = false;
		try {
			//TODO switch to batch mode in backend
			if ( purgeAtStart ) {
				purgeAll();
				if ( optimizeAfterPurge ) {
					optimize();
				}
			}
			
			BackendQueueProcessorFactory backendQueueProcessorFactory = searchFactory.getBackendQueueProcessorFactory();
			//first start the consumers, then the producers (reverse order):
			for ( int i=0; i < indexWritingThreadNum; i++ ) {
				//from LuceneWork to IndexWriters:
				execWriteIndex.execute( new IndexWritingJob(
						fromAddworkToIndex, endWritersSignal, monitor, backendQueueProcessorFactory ) );
			}
			for ( int i=0; i < luceneworkerBuildingThreadNum; i++ ) {
				//from entity to LuceneWork:
				execDocBuilding.execute( new EntityConsumerLuceneworkProducer(
						fromEntityToAddwork, fromAddworkToIndex, monitor,
						sessionFactory, searchFactory, cacheMode) );
			}
			for ( int i=0; i < objectLoadingThreadNum; i++ ) {
				//from primary key to loaded entity:
				execFirstLoader.execute( new IdentifierConsumerEntityProducer(
						fromIdentifierListToEntities, fromEntityToAddwork, monitor,
						sessionFactory, cacheMode, indexedType) );
			}
			
			execIdentifiersLoader.execute( new IdentifierProducer(fromIdentifierListToEntities, sessionFactory, objectLoadingBatchSize, indexedType, monitor) );
			//shutdown all executors:
			execIdentifiersLoader.shutdown();
			execFirstLoader.shutdown();
			execDocBuilding.shutdown();
			execWriteIndex.shutdown();
			log.debug( "helper executors are shutting down" );
			try {
				endWritersSignal.await(); //await all indexing is done.
			} catch (InterruptedException e) {
				interrupted = true;
				throw new SearchException( "Interrupted on batch Indexing; index will be left in unknown state!", e);
			}
			log.debug( "index writing finished" );
			if ( optimizeAtEnd ) {
				log.debug( "starting optimization" );
				optimize();
			}
		}
		finally {
			endAllSignal.countDown();
			if ( interrupted ) {
				//restore interruption signal:
				Thread.currentThread().interrupt();
			}
		}
	}

	private void optimize() {
		Set<Class<?>> targetedClasses = searchFactory.getIndexedTypesPolymorphic( new Class[] {indexedType} );
		List<LuceneWork> queue = new ArrayList<LuceneWork>( targetedClasses.size() );
		for ( Class<?> clazz : targetedClasses ) {
			queue.add( new OptimizeLuceneWork( clazz ) );
		}
		//TODO use the batch backend
		searchFactory.getBackendQueueProcessorFactory().getProcessor( queue ).run();
	}

	private void purgeAll() {
		Set<Class<?>> targetedClasses = searchFactory.getIndexedTypesPolymorphic( new Class[] {indexedType} );
		List<LuceneWork> queue = new ArrayList<LuceneWork>( targetedClasses.size() );
		for ( Class<?> clazz : targetedClasses ) {
			queue.add( new PurgeAllLuceneWork( clazz ) );
		}
		//TODO use the batch backend
		searchFactory.getBackendQueueProcessorFactory().getProcessor( queue ).run();
	}

}
