/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.spi.MassIndexerWithTenant;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.jmx.impl.JMXRegistrar;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.IndexedTypeSets;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.
 * The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class MassIndexerImpl implements MassIndexerWithTenant {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final ExtendedSearchIntegrator extendedIntegrator;
	private final SessionFactoryImplementor sessionFactory;

	protected final IndexedTypeSet rootEntities;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6; //loading the main entity, also responsible for loading of lazy @IndexedEmbedded collections
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeAtEnd = true;
	private boolean purgeAtStart = true;
	private boolean optimizeAfterPurge = true;
	private MassIndexerProgressMonitor monitor;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private String tenantIdentifier;
	private Integer idLoadingTransactionTimeout;

	protected MassIndexerImpl(SearchIntegrator searchIntegrator, SessionFactoryImplementor sessionFactory, Class<?>... entities) {
		this.extendedIntegrator = searchIntegrator.unwrap( ExtendedSearchIntegrator.class );
		this.sessionFactory = sessionFactory;
		rootEntities = toRootEntities( extendedIntegrator, entities );
		if ( extendedIntegrator.isJMXEnabled() ) {
			monitor = new JMXRegistrar.IndexingProgressMonitor();
		}
		else {
			monitor = new SimpleIndexingProgressMonitor();
		}
	}

	/**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 *
	 * @param selection
	 *
	 * @return a new set of entities
	 */
	private static IndexedTypeSet toRootEntities(ExtendedSearchIntegrator extendedIntegrator, Class<?>... selection) {
		//first build the "entities" set containing all indexed subtypes of "selection".
		Set<Class<?>> entities = new HashSet<>();
		Set<Class<?>> nonIndexedEntities = new HashSet<>();
		for ( Class<?> entityType : selection ) {
			IndexedTypeSet entityTypeAsSet = IndexedTypeSets.fromClass( entityType );
			IndexedTypeSet targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( entityTypeAsSet );
			if ( targetedClasses.isEmpty() ) {
				nonIndexedEntities.add( entityType );
			}
			else {
				entities.addAll( targetedClasses.toPojosSet() );
			}
		}

		if ( !nonIndexedEntities.isEmpty() ) {
			IndexedTypeSet nonIndexedEntitiesAsSet = IndexedTypeSets.fromClasses( nonIndexedEntities );
			IndexedTypeSet configuredTargetEntities =
					extendedIntegrator.getConfiguredTypesPolymorphic( nonIndexedEntitiesAsSet );
			if ( configuredTargetEntities.isEmpty() ) {
				throw log.someTargetedEntityTypesNotConfigured( StringHelper.join( nonIndexedEntities, "," ) );
			}
			else {
				throw log.someTargetedEntityTypesNotIndexed( StringHelper.join( nonIndexedEntities, "," ) );
			}
		}

		Set<Class<?>> cleaned = new HashSet<Class<?>>();
		Set<Class<?>> toRemove = new HashSet<Class<?>>();
		//now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for ( Class<?> type : entities ) {
			boolean typeIsOk = true;
			for ( Class<?> existing : cleaned ) {
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
		log.debugf( "Targets for indexing job: %s", cleaned );
		return IndexedTypeSets.fromClasses( cleaned.toArray( new Class[cleaned.size()] ) );
	}

	@Override
	public MassIndexer typesToIndexInParallel(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.typesToIndexInParallel = Math.min( numberOfThreads, rootEntities.size() );
		return this;
	}

	@Override
	public MassIndexer cacheMode(CacheMode cacheMode) {
		if ( cacheMode == null ) {
			throw new IllegalArgumentException( "cacheMode must not be null" );
		}
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public MassIndexer threadsToLoadObjects(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.documentBuilderThreads = numberOfThreads;
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
		return this;
	}

	@Override
	@Deprecated
	public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		//currently a no-op
		return this;
	}

	@Override
	public MassIndexer progressMonitor(MassIndexerProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	@Override
	public MassIndexer optimizeOnFinish(boolean optimize) {
		this.optimizeAtEnd = optimize;
		return this;
	}

	@Override
	public MassIndexer optimizeAfterPurge(boolean optimize) {
		this.optimizeAfterPurge = optimize;
		return this;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		this.purgeAtStart = purgeAll;
		return this;
	}

	@Override
	public MassIndexer transactionTimeout(int timeoutInSeconds) {
		this.idLoadingTransactionTimeout = timeoutInSeconds;
		return this;
	}

	@Override
	public MassIndexerWithTenant tenantIdentifier(String tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
		return this;
	}

	@Override
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

	@Override
	public void startAndWait() throws InterruptedException {
		BatchCoordinator coordinator = createCoordinator();
		coordinator.run();
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}
	}

	protected BatchCoordinator createCoordinator() {
		return new BatchCoordinator(
				rootEntities, extendedIntegrator, sessionFactory,
				typesToIndexInParallel, documentBuilderThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				monitor, idFetchSize, idLoadingTransactionTimeout,
				tenantIdentifier
		);
	}

	@Override
	public MassIndexer limitIndexedObjectsTo(long maximum) {
		this.objectsLimit = maximum;
		return this;
	}

	@Override
	public MassIndexer idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
		return this;
	}
}
