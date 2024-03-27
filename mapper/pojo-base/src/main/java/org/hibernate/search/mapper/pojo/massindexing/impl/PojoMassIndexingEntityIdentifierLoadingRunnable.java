/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMassIndexingEntityIdentifierLoadingRunnable<E, I>
		extends PojoMassIndexingFailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingContext massIndexingContext;
	private final PojoMassIndexingIndexedTypeGroup<E> typeGroup;
	private final PojoMassLoadingStrategy<E, I> loadingStrategy;
	private final PojoProducerConsumerQueue<List<I>> identifierQueue;
	private final String tenantId;
	private final MassIndexingEnvironment.EntityIdentifierLoadingContext identifierLoadingContext;

	public PojoMassIndexingEntityIdentifierLoadingRunnable(PojoMassIndexingNotifier notifier,
			PojoMassIndexingContext massIndexingContext, MassIndexingEnvironment environment,
			PojoMassIndexingIndexedTypeGroup<E> typeGroup,
			PojoMassLoadingStrategy<E, I> loadingStrategy,
			PojoProducerConsumerQueue<List<I>> identifierQueue, String tenantId) {
		super( notifier, environment );
		this.massIndexingContext = massIndexingContext;
		this.loadingStrategy = loadingStrategy;
		this.typeGroup = typeGroup;
		this.identifierQueue = identifierQueue;
		this.tenantId = tenantId;

		this.identifierLoadingContext = new EntityIdentifierLoadingContextImpl();
	}

	@Override
	protected void runWithFailureHandler() throws InterruptedException {
		log.trace( "started" );
		LoadingContext context = new LoadingContext();
		try ( PojoMassIdentifierLoader loader =
				loadingStrategy.createIdentifierLoader( typeGroup.includedTypes(), context ) ) {
			long totalCount = loader.totalCount();
			getNotifier().reportAddedTotalCount( totalCount );
			do {
				loader.loadNext();
			}
			while ( !context.done );
			// Only do this when stopping normally,
			// because this operation will block if the queue is full,
			// resuming the thread only if the queue gets consumed (consumer still working)
			// or if the thread is interrupted by the workspace (due to consumer failure).
			identifierQueue.producerStopping();
		}
		log.trace( "finished" );
	}

	@Override
	protected void cleanUpOnFailure() {
		// Nothing to do
	}

	@Override
	protected void cleanUpOnInterruption() {
		// Nothing to do
	}

	@Override
	protected MassIndexingEnvironment.Context createMassIndexingEnvironmentContext() {
		return identifierLoadingContext;
	}

	@Override
	protected boolean supportsThreadLifecycleHooks() {
		return true;
	}

	@Override
	protected String operationName() {
		return log.massIndexerFetchingIds( typeGroup.notifiedGroupName() );
	}

	private class LoadingContext implements PojoMassIdentifierLoadingContext<I> {
		private boolean done = false;

		@Override
		public PojoMassLoadingContext parent() {
			return massIndexingContext;
		}

		@Override
		public PojoMassIdentifierSink<I> createSink() {
			return new PojoMassIdentifierSink<I>() {
				@Override
				public void accept(List<? extends I> batch) throws InterruptedException {
					log.tracef( "produced a list of ids %s", batch );
					List<I> copy = new ArrayList<>( batch );
					identifierQueue.put( copy );
				}

				@Override
				public void complete() {
					done = true;
				}
			};
		}

		@Override
		public String tenantIdentifier() {
			return tenantId;
		}
	}

	private static final class EntityIdentifierLoadingContextImpl
			implements MassIndexingEnvironment.EntityIdentifierLoadingContext {
	}
}
