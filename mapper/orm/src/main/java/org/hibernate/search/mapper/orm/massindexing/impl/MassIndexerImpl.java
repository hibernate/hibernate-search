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

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.mapper.orm.massindexing.monitor.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkExecutor;
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

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final SessionFactoryImplementor sessionFactory;
	private final DetachedSessionContextImplementor sessionContext;

	private final Set<Class<?>> rootEntities;
	private final PojoScopeWorkExecutor scopeWorkExecutor;

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

	public MassIndexerImpl(SessionFactoryImplementor sessionFactory,
			HibernateOrmMassIndexingMappingContext mappingContext,
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<?>> targetedIndexedTypes,
			DetachedSessionContextImplementor sessionContext,
			PojoScopeWorkExecutor scopeWorkExecutor) {
		this.sessionFactory = sessionFactory;
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.rootEntities = toRootEntities( targetedIndexedTypes );
		this.scopeWorkExecutor = scopeWorkExecutor;

		// TODO HSEARCH-3057 use a JMX monitor if JMX is enabled (see Search 5)
		this.monitor = new SimpleIndexingProgressMonitor();
	}

	/*
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 */
	private static Set<Class<?>> toRootEntities(
			Set<? extends HibernateOrmMassIndexingIndexedTypeContext<?>> targetedIndexedTypeContexts) {
		Set<Class<?>> cleaned = new LinkedHashSet<>();
		Set<Class<?>> toRemove = new HashSet<>();
		//now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for ( HibernateOrmMassIndexingIndexedTypeContext<?> typeContext : targetedIndexedTypeContexts ) {
			Class<?> type = typeContext.getJavaClass();
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
	public CompletableFuture<?> start() {
		return CompletableFuture.runAsync( createCoordinator() );
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
				sessionFactory, mappingContext, sessionContext,
				rootEntities, scopeWorkExecutor,
				typesToIndexInParallel, documentBuilderThreads,
				cacheMode, objectLoadingBatchSize, objectsLimit,
				optimizeAtEnd, purgeAtStart, optimizeAfterPurge,
				monitor, idFetchSize, idLoadingTransactionTimeout
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
