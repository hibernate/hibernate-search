/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingOptions;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 */
public class PojoMassIndexingBatchIndexingWorkspace extends PojoMassIndexingFailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final MassIndexingOptions indexingOptions;
	private final int documentBuilderThreads;

	private final List<CompletableFuture<?>> identifierProducingFutures = new ArrayList<>();
	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();
	private final MassIndexingContext<?> indexingContext;
	private final MassIndexingMappingContext mappingContext;
	private final PojoMassIndexingTypeProcessor typeProcessor;
	private final PojoMassIndexingIndexedTypeGroup<?> typeGroup;
	private final String threadNamePrefix;

	PojoMassIndexingBatchIndexingWorkspace(MassIndexingOptions indexingOptions,
			MassIndexingContext<?> indexingContext,
			MassIndexingMappingContext mappingContext,
			PojoMassIndexingNotifier notifier,
			PojoMassIndexingIndexedTypeGroup<?> typeGroup,
			int documentBuilderThreads) {
		super( notifier );
		this.indexingOptions = indexingOptions;
		this.typeGroup = typeGroup;

		//thread pool sizing:
		this.documentBuilderThreads = documentBuilderThreads;

		this.threadNamePrefix = indexingOptions.threadNamePrefix();
		this.indexingContext = indexingContext;
		this.mappingContext = mappingContext;

		//type options for dsl index invoke
		typeProcessor = new PojoMassIndexingTypeProcessor(
				notifier,
				typeGroup );

	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !identifierProducingFutures.isEmpty() || !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchIndexingWorkspace instance not expected to be reused" );
		}

		// First start the consumers, then the producers (reverse order):
		startIndexing();
		startProducingPrimaryKeys();
		// Wait for indexing to finish.
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
		);
		log.debugf( "Indexing for %s is done", typeGroup.notifiedGroupName() );
	}

	@Override
	protected void cleanUpOnInterruption() {
		cancelPendingTasks();
	}

	@Override
	protected void cleanUpOnFailure() {
		cancelPendingTasks();
	}

	private void cancelPendingTasks() {
		// Cancel each pending task - threads executing the tasks must be interrupted
		for ( Future<?> task : identifierProducingFutures ) {
			task.cancel( true );
		}
		for ( Future<?> task : indexingFutures ) {
			task.cancel( true );
		}
	}

	private void startProducingPrimaryKeys() {
		final Runnable primaryKeyOutputter = new PojoMassIndexingFailureInterceptingHadler(
				indexingOptions,
				indexingContext.identifierInterceptors(),
				getNotifier(),
				typeProcessor.identifierProducer() );
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor identifierProducingExecutor = mappingContext.threadPoolProvider().newFixedThreadPool(
				1,
				threadNamePrefix + typeGroup.notifiedGroupName() + " - ID loading"
		);
		try {
			identifierProducingFutures.add( Futures.runAsync( primaryKeyOutputter, identifierProducingExecutor ) );
		}
		finally {
			identifierProducingExecutor.shutdown();
		}
	}

	private void startIndexing() {
		final Runnable documentOutputter = new PojoMassIndexingFailureInterceptingHadler(
				indexingOptions,
				indexingContext.documentInterceptors(),
				getNotifier(),
				typeProcessor.documentProducer() );
		final ThreadPoolExecutor indexingExecutor = mappingContext.threadPoolProvider().newFixedThreadPool(
				documentBuilderThreads,
				threadNamePrefix + typeGroup.notifiedGroupName() + " - Entity loading"
		);
		try {
			for ( int i = 0; i < documentBuilderThreads; i++ ) {
				indexingFutures.add( Futures.runAsync( documentOutputter, indexingExecutor ) );
			}
		}
		finally {
			indexingExecutor.shutdown();
		}
	}
}
