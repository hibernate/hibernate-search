package org.hibernate.search.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.Indexer;
import org.hibernate.search.batchindexing.BatchIndexingWorkspace;
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
	protected List<BatchIndexingWorkspace> indexers = new LinkedList<BatchIndexingWorkspace>();
	boolean started = false; 
	private CountDownLatch endAllSignal;
	
	// default settings defined here:
	private int objectLoadingThreads = 2; //loading the main entity
	private int collectionLoadingThreads = 4; //also responsible for loading of lazy @IndexedEmbedded collections
	private int writerThreads = 1; //also running the Analyzers
	private int objectLoadingBatchSize = 10;
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
		if ( log.isDebugEnabled() ) {
			log.debug( "Targets for indexing job: {}", cleaned );
		}
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
	
	public void start() {
		if ( started ) {
			throw new IllegalStateException( "Can be started only once" );
		}
		else {
			started = true;
			endAllSignal = new CountDownLatch( rootEntities.size() );
			for ( Class<?> type : rootEntities ) {
				indexers.add( new BatchIndexingWorkspace(
						searchFactoryImplementor, sessionFactory, type,
						objectLoadingThreads, collectionLoadingThreads, writerThreads,
						cacheMode, objectLoadingBatchSize,
						optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
						endAllSignal, monitor) );
			}
			for ( BatchIndexingWorkspace batcher : indexers ) {
				new Thread( batcher ).start();
			}
		}
	}

	public boolean startAndWait(long timeout, TimeUnit unit) throws InterruptedException {
		start();
		return endAllSignal.await( timeout, unit );
	}

}
