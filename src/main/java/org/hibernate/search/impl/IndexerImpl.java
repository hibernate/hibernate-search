package org.hibernate.search.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.Indexer;
import org.hibernate.search.batchindexing.BatchCoordinator;
import org.hibernate.search.batchindexing.Executors;
import org.hibernate.search.batchindexing.IndexerProgressMonitor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entities in the database.
 * 
 * @author Sanne Grinovero
 */
public class IndexerImpl implements Indexer {
	
	private static final Logger log = LoggerFactory.make();
	
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final SessionFactory sessionFactory;

	protected Set<Class<?>> rootEntities = new HashSet<Class<?>>();
	
	// default settings defined here:
	private int objectLoadingThreads = 2; //loading the main entity
	private int collectionLoadingThreads = 4; //also responsible for loading of lazy @IndexedEmbedded collections
	private int writerThreads = 1; //also running the Analyzers
	private int objectLoadingBatchSize = 10;
	private int objectsLimit = 0; //means no limit at all
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeAtEnd = true;
	private boolean purgeAtStart = true;
	private boolean optimizeAfterPurge = true;
	private IndexerProgressMonitor monitor = new SimpleIndexingProgressMonitor();

	protected IndexerImpl(SearchFactoryImplementor searchFactory, SessionFactory sessionFactory, Class<?>...entities) {
		this.searchFactoryImplementor = searchFactory;
		this.sessionFactory = sessionFactory;
		rootEntities = toRootEntities( searchFactoryImplementor, entities );
	}

	/**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 * @param selection
	 * @return a new set of entities
	 */
	private static Set<Class<?>> toRootEntities(SearchFactoryImplementor searchFactoryImplementor, Class<?>... selection) {
		Set<Class<?>> entities = new HashSet<Class<?>>();
		//first build the "entities" set containing all indexed subtypes of "selection".
		for (Class<?> entityType : selection) {
			Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic( new Class[] {entityType} );
			if ( targetedClasses.isEmpty() ) {
				String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
				throw new IllegalArgumentException( msg );
			}
			entities.addAll( targetedClasses );
		}
		Set<Class<?>> cleaned = new HashSet<Class<?>>();
		Set<Class<?>> toRemove = new HashSet<Class<?>>();
		//now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for (Class<?> type : entities) {
			boolean typeIsOk = true;
			for (Class<?> existing : cleaned) {
				if ( existing.isAssignableFrom( type ) ) {
					typeIsOk = false;
					break;
				}
				if ( type.isAssignableFrom( existing ) ) {
					toRemove.add( existing );
				}
			}
			if ( typeIsOk ) {
				cleaned.add( type );
			}
		}
		cleaned.removeAll( toRemove );
		log.debug( "Targets for indexing job: {}", cleaned );
		return cleaned;
	}

	public Indexer cacheMode(CacheMode cacheMode) {
		if ( cacheMode == null )
			throw new IllegalArgumentException( "cacheMode must not be null" );
		this.cacheMode = cacheMode;
		return this;
	}

	public Indexer objectLoadingThreads(int numberOfThreads) {
		if ( numberOfThreads < 1 )
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		this.objectLoadingThreads = numberOfThreads;
		return this;
	}
	
	public Indexer objectLoadingBatchSize(int batchSize) {
		if ( batchSize < 1 )
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		this.objectLoadingBatchSize = batchSize;
		return this;
	}
	
	public Indexer documentBuilderThreads(int numberOfThreads) {
		if ( numberOfThreads < 1 )
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		this.collectionLoadingThreads = numberOfThreads;
		return this;
	}
	
	public Indexer indexWriterThreads(int numberOfThreads) {
		if ( numberOfThreads < 1 )
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		this.writerThreads = numberOfThreads;
		return this;
	}
	
	public Indexer optimizeAtEnd(boolean optimize) {
		this.optimizeAtEnd = optimize;
		return this;
	}

	public Indexer optimizeAfterPurge(boolean optimize) {
		this.optimizeAfterPurge = optimize;
		return this;
	}

	public Indexer purgeAllAtStart(boolean purgeAll) {
		this.purgeAtStart = purgeAll;
		return this;
	}
	
	public Future<?> start() {
		BatchCoordinator coordinator = createCoordinator();
		ExecutorService executor = Executors.newFixedThreadPool( 1, "batch coordinator" );
		try {
			Future<?> submit = executor.submit( coordinator );
			return submit;
		}
		finally {
			executor.shutdown();
		}
	}
	
	public void startAndWait() throws InterruptedException {
		BatchCoordinator coordinator = createCoordinator();
		coordinator.run();
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}
	}
	
	protected BatchCoordinator createCoordinator() {
		return new BatchCoordinator( rootEntities, searchFactoryImplementor, sessionFactory,
				objectLoadingThreads, collectionLoadingThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				monitor );
	}

	public Indexer limitObjects(int maximum) {
		this.objectsLimit = maximum;
		return this;
	}

}
