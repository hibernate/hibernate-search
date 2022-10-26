/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * A strategy for sending indexing events to a remote processor,
 * which will use a {@link PojoIndexingPlanEventProcessingStrategy}.
 */
public class PojoIndexingPlanEventSendingStrategy implements PojoIndexingPlanStrategy {
	private final PojoIndexingQueueEventSendingPlan sendingPlan;

	public PojoIndexingPlanEventSendingStrategy(PojoIndexingQueueEventSendingPlan sendingPlan) {
		this.sendingPlan = sendingPlan;
	}

	@Override
	public boolean shouldResolveDirtyForDeleteOnly() {
		// When possible, we will resolve dirty entities to reindex in the background process
		// that consumes the events we're sending.
		// For deletes, though, we cannot do that, so we resolve dirty entities directly in-session.
		return true;
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> doExecuteAndReport(
			Collection<PojoIndexedTypeIndexingPlan<?, ?>> indexedTypeDelegates,
			PojoLoadingPlanProvider loadingPlanProvider, EntityReferenceFactory<R> entityReferenceFactory,
			OperationSubmitter operationSubmitter) {

		// No need to go through every single type: the state is global.
		return sendingPlan.sendAndReport( entityReferenceFactory, operationSubmitter );
	}

	@Override
	public void doDiscard(Collection<PojoIndexedTypeIndexingPlan<?, ?>> indexedTypeDelegates) {
		// No need to go through every single type: the state is global.
		sendingPlan.discard();
	}

	@Override
	public <I, E> PojoIndexedTypeIndexingPlan<I, E> createIndexedDelegate(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext,
			PojoIndexingProcessorRootContext processorContext) {
		// Will send indexing events to an external queue.
		return new PojoIndexedTypeIndexingPlan<>( typeContext, sessionContext,
				new PojoTypeIndexingPlanEventQueueDelegate<>( typeContext, sessionContext, sendingPlan ) );
	}

	@Override
	public <I, E> PojoContainedTypeIndexingPlan<I, E> createDelegate(PojoWorkContainedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext) {
		// Will send indexing events to an external queue.
		return new PojoContainedTypeIndexingPlan<>( typeContext, sessionContext,
				new PojoTypeIndexingPlanEventQueueDelegate<>( typeContext, sessionContext, sendingPlan ) );
	}
}
