/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.util.impl.common.Executors;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.StringHelper;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.
 * The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class MassIndexerImpl implements MassIndexer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmMapping mapping;
	private final SessionFactoryImplementor sessionFactory;
	private final String tenantIdentifier;

	protected final Set<Class<?>> rootEntities;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6; //loading the main entity, also responsible for loading of lazy @IndexedEmbedded collections
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeAtEnd = true;
	private boolean purgeAtStart = true;
	private boolean optimizeAfterPurge = true;
	//TODO: restore the MassIndexerProgressMonitor
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private Integer idLoadingTransactionTimeout;

	public MassIndexerImpl(SessionFactoryImplementor sessionFactory, String tenantIdentifier, Class<?>... entities) {
		this.sessionFactory = sessionFactory;
		this.mapping = sessionFactory.getServiceRegistry().getService( HibernateSearchContextService.class ).getMapping();
		this.tenantIdentifier = tenantIdentifier;
		this.rootEntities = toRootEntities( mapping, entities );
		//TODO: restore the MassIndexerProgressMonitor. Using a JMXRegistrar or a SimpleIndexingProgressMonitor
	}

	/**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 *
	 * @param mapping
	 * @param selection
	 *
	 * @return a new set of entities
	 */
	private static Set<Class<?>> toRootEntities(HibernateOrmMapping mapping, Class<?>... selection) {
		//first build the "entities" set containing all indexed subtypes of "selection".
		Set<Class<?>> entities = new HashSet<>();
		Set<Class<?>> nonIndexedEntities = new HashSet<>();
		for ( Class<?> entityType : selection ) {
			Set<? extends Class<?>> indexedTypesPolymorphic = mapping.getIndexedTypesPolymorphic( entityType );
			if ( indexedTypesPolymorphic.isEmpty() ) {
				nonIndexedEntities.add( entityType );
			}
			else {
				entities.addAll( indexedTypesPolymorphic );
			}
		}

		// check for not configured entities
		for ( Class<?> nonIndexedEntity : nonIndexedEntities ) {
			if ( !mapping.isWorkable( nonIndexedEntity ) ) {
				throw log.someTargetedEntityTypesNotConfigured( StringHelper.join( nonIndexedEntities, "," ) );
			}
		}

		// check for not indexed entities
		if ( !nonIndexedEntities.isEmpty() ) {
			throw log.someTargetedEntityTypesNotIndexed( StringHelper.join( nonIndexedEntities, "," ) );
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
		return cleaned;
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
				rootEntities, sessionFactory, mapping,
				typesToIndexInParallel, documentBuilderThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				idFetchSize, idLoadingTransactionTimeout,
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
