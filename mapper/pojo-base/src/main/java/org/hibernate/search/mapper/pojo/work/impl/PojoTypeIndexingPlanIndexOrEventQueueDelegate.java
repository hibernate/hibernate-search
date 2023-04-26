/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * A {@link PojoTypeIndexingPlanDelegate} that sends indexing events to the index,
 * to process them locally,
 * except when those events were caused exclusively by a change in a contained entity,
 * in which case it sends indexing events to an external queue,
 * to process them externally.
 *
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 */
final class PojoTypeIndexingPlanIndexOrEventQueueDelegate<I, E> implements PojoTypeIndexingPlanDelegate<I, E> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final PojoTypeIndexingPlanIndexDelegate<I, E> indexDelegate;
	private final PojoTypeIndexingPlanEventQueueDelegate<I, E> eventQueueDelegate;

	PojoTypeIndexingPlanIndexOrEventQueueDelegate(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoIndexingProcessorRootContext processorRootContext,
			IndexIndexingPlan indexPlan, PojoIndexingQueueEventSendingPlan sendingPlan) {
		this.typeContext = typeContext;
		this.indexDelegate = new PojoTypeIndexingPlanIndexDelegate<>( typeContext, sessionContext, processorRootContext,
				indexPlan );
		this.eventQueueDelegate = new PojoTypeIndexingPlanEventQueueDelegate<>( typeContext, sessionContext, sendingPlan );
	}

	@Override
	public boolean isDirtyForAddOrUpdate(boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPathsOrNull) {
		// We will execute the addOrUpdate below
		// if the dirty paths require the entity itself to be reindexed,
		// but not if they only require reindexing some containing entities.
		// Contained entities will be handled through reindexing resolution.
		return forceSelfDirty
				|| dirtyPathsOrNull != null && typeContext.dirtySelfFilter().test( dirtyPathsOrNull );
	}

	@Override
	public void add(I identifier, DocumentRouteDescriptor route, Supplier<E> entitySupplier) {
		indexDelegate.add( identifier, route, entitySupplier );
	}

	@Override
	public void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths,
			boolean updatedBecauseOfContained, boolean updateBecauseOfDirty) {
		PojoTypeIndexingPlanDelegate<I, E> delegate;
		if ( updatedBecauseOfContained && !updateBecauseOfDirty ) {
			// The entity needs to be updated because of a contained entity,
			// but there was no processed event for this entity proper
			// (otherwise selfDirty would be true - see
			// org.hibernate.search.mapper.pojo.work.impl.PojoIndexingQueueEventProcessingPlanImpl.addOrUpdate).
			// In order to ensure that a given entity instance is always processed
			// by the same background process, we will send an event to be processed later.
			delegate = eventQueueDelegate;
		}
		else {
			delegate = indexDelegate;
		}
		delegate.addOrUpdate( identifier, routes, entitySupplier,
				forceSelfDirty, forceContainingDirty, dirtyPaths, updatedBecauseOfContained, updateBecauseOfDirty
		);
	}

	@Override
	public void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		indexDelegate.delete( identifier, routes, entitySupplier );
	}

	@Override
	public void discard() {
		indexDelegate.discard();
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter) {
		return indexDelegate.executeAndReport( operationSubmitter );
	}

}
