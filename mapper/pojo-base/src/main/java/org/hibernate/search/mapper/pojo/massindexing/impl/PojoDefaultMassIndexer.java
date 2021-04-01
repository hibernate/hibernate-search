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
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingMappingContext;
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
 * @param <O> The options type.
 */
public class PojoDefaultMassIndexer<O> implements PojoMassIndexer<O> {

	private final MassIndexingContext<O> massIndexingConfigurationContext;
	private final MassIndexingMappingContext mappingContext;

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
	private final List<PojoMassIndexingIndexedTypeGroup<?, O>> typeGroupsToIndex;

	public PojoDefaultMassIndexer(MassIndexingContext<O> indexingContext,
			MassIndexingMappingContext mappingContext,
			MassIndexingTypeContextProvider typeContextProvider,
			Set<? extends MassIndexingIndexedTypeContext<?>> targetedIndexedTypes,
			PojoScopeSchemaManager scopeSchemaManager,
			PojoScopeWorkspace scopeWorkspace) {
		this.massIndexingConfigurationContext = indexingContext;
		this.mappingContext = mappingContext;
		this.typeGroupsToIndex = PojoMassIndexingIndexedTypeGroup.disjoint( indexingContext,
				mappingContext, typeContextProvider, targetedIndexedTypes );
		this.scopeSchemaManager = scopeSchemaManager;
		this.scopeWorkspace = scopeWorkspace;
	}

	@Override
	public PojoDefaultMassIndexer<O> typesToIndexInParallel(int numberOfThreads) {
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
	public PojoDefaultMassIndexer<O> threadsToLoadObjects(int numberOfThreads) {
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
	public PojoDefaultMassIndexer<O> mergeSegmentsOnFinish(boolean enable) {
		this.mergeSegmentsOnFinish = enable;
		return this;
	}

	public boolean mergeSegmentsOnFinish() {
		return mergeSegmentsOnFinish;
	}

	@Override
	public PojoDefaultMassIndexer<O> mergeSegmentsAfterPurge(boolean enable) {
		this.mergeSegmentsAfterPurge = enable;
		return this;
	}

	public boolean mergeSegmentsAfterPurge() {
		return mergeSegmentsAfterPurge;
	}

	@Override
	public PojoDefaultMassIndexer<O> dropAndCreateSchemaOnStart(boolean enable) {
		this.dropAndCreateSchemaOnStart = enable;
		return this;
	}

	public boolean dropAndCreateSchemaOnStart() {
		return dropAndCreateSchemaOnStart;
	}

	@Override
	public PojoDefaultMassIndexer<O> purgeAllOnStart(boolean enable) {
		this.purgeAtStart = enable;
		return this;
	}

	public boolean purgeAtStart() {
		return purgeAtStart;
	}

	@Override
	public PojoDefaultMassIndexer<O> monitor(MassIndexingMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	public MassIndexingMonitor monitor() {
		return monitor;
	}

	@Override
	public CompletableFuture<?> start(O options) {
		PojoMassIndexingBatchCoordinator<O> coordinator = createCoordinator( options );
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
	public void startAndWait(O options) throws InterruptedException {
		PojoMassIndexingBatchCoordinator<O> coordinator = createCoordinator( options );
		coordinator.run();
		if ( Thread.interrupted() ) {
			throw new InterruptedException();
		}
	}

	private PojoMassIndexingBatchCoordinator<O> createCoordinator(O options) {
		PojoMassIndexingNotifier notifier = new PojoMassIndexingNotifier(
				getOrCreateFailureHandler(),
				getOrCreateMonitor()
		);
		return new PojoMassIndexingBatchCoordinator<>(
				options, massIndexingConfigurationContext, mappingContext,
				notifier,
				typeGroupsToIndex, scopeSchemaManager, scopeWorkspace,
				typesToIndexInParallel, documentBuilderThreads,
				mergeSegmentsOnFinish, dropAndCreateSchemaOnStart,
				purgeAtStart, mergeSegmentsAfterPurge
		);
	}

	@Override
	public PojoDefaultMassIndexer<O> failureHandler(MassIndexingFailureHandler failureHandler) {
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
