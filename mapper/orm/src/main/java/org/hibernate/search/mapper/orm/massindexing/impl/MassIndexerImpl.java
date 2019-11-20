/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.mapper.orm.massindexing.monitor.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	static final String THREAD_NAME_PREFIX = "Mass indexing - ";

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final DetachedBackendSessionContext sessionContext;

	private final Set<HibernateOrmMassIndexingIndexedTypeContext<?>> rootEntityTypes;
	private final PojoScopeWorkspace scopeWorkspace;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6; //loading the main entity, also responsible for loading of lazy @IndexedEmbedded collections
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeAtEnd = true;
	private boolean purgeAtStart = true;
	private boolean optimizeAfterPurge = true;
	private MassIndexingMonitor monitor;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private Integer idLoadingTransactionTimeout;

	public MassIndexerImpl(HibernateOrmMassIndexingMappingContext mappingContext,
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<?>> targetedIndexedTypes,
			DetachedBackendSessionContext sessionContext,
			PojoScopeWorkspace scopeWorkspace) {
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.rootEntityTypes = toRootEntityTypes( mappingContext.getSessionFactory(), targetedIndexedTypes );
		this.scopeWorkspace = scopeWorkspace;
	}

	/*
	 * From the set of targeted types a new set is built, removing all subtypes of indexed entities.
	 */
	private static Set<HibernateOrmMassIndexingIndexedTypeContext<?>> toRootEntityTypes(
			SessionFactoryImplementor sessionFactory,
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<?>> targetedIndexedTypeContexts) {
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();

		Set<HibernateOrmMassIndexingIndexedTypeContext<?>> cleaned = new LinkedHashSet<>();
		Set<HibernateOrmMassIndexingIndexedTypeContext<?>> toRemove = new HashSet<>();
		//now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for ( HibernateOrmMassIndexingIndexedTypeContext<?> typeContext : targetedIndexedTypeContexts ) {
			EntityTypeDescriptor<?> entityType = typeContext.getEntityType();
			boolean typeIsOk = true;
			for ( HibernateOrmMassIndexingIndexedTypeContext<?> existing : cleaned ) {
				EntityTypeDescriptor<?> existingEntityType = existing.getEntityType();
				if ( HibernateOrmUtils.isSuperTypeOf( metamodel, existingEntityType, entityType ) ) {
					typeIsOk = false;
					break;
				}
				if ( HibernateOrmUtils.isSuperTypeOf( metamodel, entityType, existingEntityType ) ) {
					toRemove.add( existing );
				}
			}
			if ( typeIsOk ) {
				cleaned.add( typeContext );
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
		this.typesToIndexInParallel = Math.min( numberOfThreads, rootEntityTypes.size() );
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
	public MassIndexer monitor(MassIndexingMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	@Override
	public CompletableFuture<?> start() {
		BatchCoordinator coordinator = createCoordinator();
		ExecutorService executor = mappingContext.getThreadPoolProvider()
				.newFixedThreadPool( 1, THREAD_NAME_PREFIX + "Coordinator" );
		try {
			return Futures.runAsync( coordinator, executor );
		}
		finally {
			executor.shutdown();
		}

	}

	@Override
	public void startAndWait() throws InterruptedException {
		BatchCoordinator coordinator = createCoordinator();
		coordinator.run();
		if ( Thread.interrupted() ) {
			throw new InterruptedException();
		}
	}

	protected BatchCoordinator createCoordinator() {
		MassIndexingNotifier notifier = new MassIndexingNotifier(
				mappingContext.getFailureHandler(),
				getOrCreateMonitor()
		);
		return new BatchCoordinator(
				mappingContext, sessionContext,
				notifier,
				rootEntityTypes, scopeWorkspace,
				typesToIndexInParallel, documentBuilderThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				idFetchSize, idLoadingTransactionTimeout
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

	private MassIndexingMonitor getOrCreateMonitor() {
		if ( monitor != null ) {
			return monitor;
		}

		// TODO HSEARCH-3057 use a JMX monitor if JMX is enabled (see Search 5)
		return new SimpleIndexingProgressMonitor();
	}
}
