/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;

import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.impl.Futures;

/**
 * Prepares and configures a BatchIndexingWorkspace to start rebuilding
 * the indexes for all entity instances in the database.The type of these entities is either all indexed entities or a
 * subset, always including all subtypes.
 *
 * @author Sanne Grinovero
 */
public class PojoDefaultMassIndexer implements PojoMassIndexer {

	private final PojoMassIndexingMappingContext mappingContext;

	private final PojoScopeSchemaManager scopeSchemaManager;
	private final PojoScopeWorkspace scopeWorkspace;

	// default settings defined here:
	private int typesToIndexInParallel = 1;
	private int documentBuilderThreads = 6;
	private boolean mergeSegmentsOnFinish = false;
	private boolean dropAndCreateSchemaOnStart = false;
	private boolean purgeAtStart = true;
	private boolean mergeSegmentsAfterPurge = true;

	private MassIndexingFailureHandler failureHandler;
	private MassIndexingMonitor monitor;
	private final List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex;

	public PojoDefaultMassIndexer(PojoMassIndexingContext indexingContext,
			PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingTypeContextProvider typeContextProvider,
			Set<? extends PojoMassIndexingIndexedTypeContext<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			PojoScopeWorkspace scopeWorkspace) {
		this.mappingContext = mappingContext;
		this.typeGroupsToIndex = PojoMassIndexingIndexedTypeGroup.disjoint( indexingContext,
				mappingContext, typeContextProvider, targetedIndexedTypes );
		this.scopeSchemaManager = scopeSchemaManager;
		this.scopeWorkspace = scopeWorkspace;
	}

	@Override
	public PojoDefaultMassIndexer typesToIndexInParallel(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
		this.typesToIndexInParallel = Math.min( numberOfThreads, typeGroupsToIndex.size() );
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
				mappingContext,
				notifier,
				typeGroupsToIndex, scopeSchemaManager, scopeWorkspace,
				typesToIndexInParallel, documentBuilderThreads,
				mergeSegmentsOnFinish, dropAndCreateSchemaOnStart,
				purgeAtStart, mergeSegmentsAfterPurge
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
