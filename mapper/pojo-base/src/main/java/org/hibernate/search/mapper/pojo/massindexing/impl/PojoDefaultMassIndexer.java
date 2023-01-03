/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class PojoDefaultMassIndexer implements PojoMassIndexer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingContext indexingContext;
	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexingTypeContextProvider typeContextProvider;
	private final Set<? extends PojoMassIndexingIndexedTypeContext<?>> targetedIndexedTypes;
	private final PojoScopeSchemaManager scopeSchemaManager;
	private final Collection<String> tenantIds;
	private final PojoScopeDelegate<?, ?, ?> pojoScopeDelegate;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6;
	private boolean mergeSegmentsOnFinish = false;
	private Boolean dropAndCreateSchemaOnStart;
	private Boolean purgeAtStart;
	private boolean mergeSegmentsAfterPurge = true;

	private MassIndexingFailureHandler failureHandler;
	private MassIndexingMonitor monitor;

	public PojoDefaultMassIndexer(PojoMassIndexingContext indexingContext,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			Set<? extends PojoMassIndexingIndexedTypeContext<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			Collection<String> tenantIds,
			PojoScopeDelegate<?, ?, ?> pojoScopeDelegate) {
		this.indexingContext = indexingContext;
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextProvider;
		this.targetedIndexedTypes = targetedIndexedTypes;
		this.scopeSchemaManager = scopeSchemaManager;
		this.tenantIds = tenantIds;
		this.pojoScopeDelegate = pojoScopeDelegate;
	}

	@Override
	public PojoDefaultMassIndexer typesToIndexInParallel(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.typesToIndexInParallel = numberOfThreads;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer threadsToLoadObjects(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.documentBuilderThreads = numberOfThreads;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer mergeSegmentsOnFinish(boolean enable) {
		this.mergeSegmentsOnFinish = enable;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer mergeSegmentsAfterPurge(boolean enable) {
		this.mergeSegmentsAfterPurge = enable;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer dropAndCreateSchemaOnStart(boolean enable) {
		this.dropAndCreateSchemaOnStart = enable;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer purgeAllOnStart(boolean enable) {
		this.purgeAtStart = enable;
		return this;
	}

	@Override
	public PojoDefaultMassIndexer monitor(MassIndexingMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	@Override
	public CompletableFuture<?> start() {
		PojoMassIndexingBatchCoordinator coordinator = createCoordinator();
		ExecutorService executor = mappingContext.threadPoolProvider()
				.newFixedThreadPool( 1,
						PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + "Coordinator" );
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
		try {
			coordinator.run();
		}
		catch (Throwable t) {
			if ( Thread.interrupted() ) {
				InterruptedException exception = new InterruptedException();
				exception.addSuppressed( t );
				throw exception;
			}
			else {
				throw t;
			}
		}
	}

	private PojoMassIndexingBatchCoordinator createCoordinator() {
		List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex = PojoMassIndexingIndexedTypeGroup.disjoint(
				indexingContext, mappingContext, typeContextProvider, targetedIndexedTypes
		);
		typesToIndexInParallel = Math.min( typesToIndexInParallel, typeGroupsToIndex.size() );
		PojoMassIndexingNotifier notifier = new PojoMassIndexingNotifier(
				getOrCreateFailureHandler(),
				getOrCreateMonitor()
		);

		if ( Boolean.TRUE.equals( dropAndCreateSchemaOnStart ) && Boolean.TRUE.equals( purgeAtStart ) ) {
			log.redundantPurgeAfterDrop();
		}

		return new PojoMassIndexingBatchCoordinator(
				mappingContext,
				notifier,
				typeGroupsToIndex, scopeSchemaManager,
				tenantIds, pojoScopeDelegate,
				typesToIndexInParallel, documentBuilderThreads,
				mergeSegmentsOnFinish,
				// false by default:
				Boolean.TRUE.equals( dropAndCreateSchemaOnStart ),
				// false if not set explicitly and dropAndCreateSchemaOnStart is set to true, otherwise true by default:
				purgeAtStart == null ? !Boolean.TRUE.equals( dropAndCreateSchemaOnStart ) : purgeAtStart,
				mergeSegmentsAfterPurge
		);
	}

	@Override
	public PojoDefaultMassIndexer failureHandler(MassIndexingFailureHandler failureHandler) {
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
