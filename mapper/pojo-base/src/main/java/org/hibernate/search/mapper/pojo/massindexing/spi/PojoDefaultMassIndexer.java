/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.mapper.pojo.massindexing.PojoMassIndexer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingBatchCoordinator;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingDelegatingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingFailSafeFailureHandlerWrapper;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingIndexedTypeGroup;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingLoggingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.impl.PojoMassIndexingNotifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingOptions;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class PojoDefaultMassIndexer implements PojoMassIndexer, MassIndexingOptions {

	static final String THREAD_NAME_PREFIX = "Mass indexing - ";

	private final MassIndexingContext<?> massIndexingConfigurationContext;
	private final MassIndexingMappingContext mappingContext;
	private final DetachedBackendSessionContext sessionContext;
	private final MassIndexingOptions indexerContext;

	private final PojoScopeSchemaManager scopeSchemaManager;
	private final PojoScopeWorkspace scopeWorkspace;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6;
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all
	private boolean mergeSegmentsOnFinish = false;
	private boolean dropAndCreateSchemaOnStart = false;
	private boolean purgeAtStart = true;
	private boolean mergeSegmentsAfterPurge = true;
	private int idFetchSize = 100; //reasonable default as we only load IDs

	private MassIndexingFailureHandler failureHandler;
	private MassIndexingMonitor monitor;
	private final List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex;

	public PojoDefaultMassIndexer(
			MassIndexingOptions indexerContext,
			MassIndexingContext<?> indexingContext,
			MassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			Set<? extends PojoRawTypeIdentifier<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			PojoScopeWorkspace scopeWorkspace) {
		this.massIndexingConfigurationContext = indexingContext;
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.typeGroupsToIndex = PojoMassIndexingIndexedTypeGroup.disjoint( indexingContext,
				mappingContext, targetedIndexedTypes );
		this.scopeSchemaManager = scopeSchemaManager;
		this.scopeWorkspace = scopeWorkspace;
		this.indexerContext = indexerContext != null ? indexerContext : this;
	}

	@Override
	public String threadNamePrefix() {
		return THREAD_NAME_PREFIX;
	}

	@Override
	public String tenantIdentifier() {
		return sessionContext.tenantIdentifier();
	}

	@Override
	public PojoMassIndexer typesToIndexInParallel(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.typesToIndexInParallel = Math.min( numberOfThreads, typeGroupsToIndex.size() );
		return this;
	}

	public int typesToIndexInParallel() {
		return typesToIndexInParallel;
	}

	@Override
	public PojoMassIndexer threadsToLoadObjects(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.documentBuilderThreads = numberOfThreads;
		return this;
	}

	public int threadsToLoadObjects() {
		return documentBuilderThreads;
	}

	@Override
	public PojoMassIndexer batchSizeToLoadObjects(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
		return this;
	}

	public int batchSizeToLoadObjects() {
		return objectLoadingBatchSize;
	}

	@Override
	public PojoMassIndexer mergeSegmentsOnFinish(boolean enable) {
		this.mergeSegmentsOnFinish = enable;
		return this;
	}

	public boolean mergeSegmentsOnFinish() {
		return mergeSegmentsOnFinish;
	}

	@Override
	public PojoMassIndexer mergeSegmentsAfterPurge(boolean enable) {
		this.mergeSegmentsAfterPurge = enable;
		return this;
	}

	public boolean mergeSegmentsAfterPurge() {
		return mergeSegmentsAfterPurge;
	}

	@Override
	public PojoMassIndexer dropAndCreateSchemaOnStart(boolean enable) {
		this.dropAndCreateSchemaOnStart = enable;
		return this;
	}

	public boolean dropAndCreateSchemaOnStart() {
		return dropAndCreateSchemaOnStart;
	}

	@Override
	public PojoMassIndexer purgeAllOnStart(boolean enable) {
		this.purgeAtStart = enable;
		return this;
	}

	public boolean purgeAtStart() {
		return purgeAtStart;
	}

	@Override
	public int batchSize() {
		return batchSizeToLoadObjects();
	}

	@Override
	public int fetchSize() {
		return idFetchSize();
	}

	@Override
	public PojoMassIndexer monitor(MassIndexingMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	public MassIndexingMonitor monitor() {
		return monitor;
	}

	@Override
	public CompletableFuture<?> start() {
		PojoMassIndexingBatchCoordinator coordinator = createCoordinator();
		ExecutorService executor = mappingContext.threadPoolProvider()
				.newFixedThreadPool( 1, threadNamePrefix() + "Coordinator" );
		try {
			return Futures.runAsync( coordinator, executor );
		}
		finally {
			executor.shutdown();
		}

	}

	@Override
	public void startAndWait() throws InterruptedException {
		PojoMassIndexingBatchCoordinator coordinator = createCoordinator();
		coordinator.run();
		if ( Thread.interrupted() ) {
			throw new InterruptedException();
		}
	}

	private PojoMassIndexingBatchCoordinator createCoordinator() {
		PojoMassIndexingNotifier notifier = new PojoMassIndexingNotifier(
				getOrCreateFailureHandler(),
				getOrCreateMonitor()
		);
		return new PojoMassIndexingBatchCoordinator(
				indexerContext, massIndexingConfigurationContext, mappingContext,
				notifier,
				typeGroupsToIndex, scopeSchemaManager, scopeWorkspace,
				typesToIndexInParallel, documentBuilderThreads,
				mergeSegmentsOnFinish, dropAndCreateSchemaOnStart,
				purgeAtStart, mergeSegmentsAfterPurge
		);
	}

	@Override
	public PojoMassIndexer limitIndexedObjectsTo(long maximum) {
		this.objectsLimit = maximum;
		return this;
	}

	@Override
	public long objectsLimit() {
		return objectsLimit;
	}

	@Override
	public PojoMassIndexer idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
		return this;
	}

	public int idFetchSize() {
		return idFetchSize;
	}

	@Override
	public PojoMassIndexer failureHandler(MassIndexingFailureHandler failureHandler) {
		this.failureHandler = failureHandler;
		return this;
	}

	private MassIndexingFailureHandler getOrCreateFailureHandler() {
		MassIndexingFailureHandler result = failureHandler;
		if ( result == null ) {
			result = new PojoMassIndexingDelegatingFailureHandler( mappingContext.failureHandler() );
		}
		result = new PojoMassIndexingFailSafeFailureHandlerWrapper( result );
		return result;
	}

	private MassIndexingMonitor getOrCreateMonitor() {
		if ( monitor != null ) {
			return monitor;
		}
		return new PojoMassIndexingLoggingMonitor();
	}
}
