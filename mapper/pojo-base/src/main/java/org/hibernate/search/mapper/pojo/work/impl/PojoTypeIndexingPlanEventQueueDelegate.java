/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.mapper.pojo.work.spi.DirtinessDescriptor;
import org.hibernate.search.util.common.AssertionFailure;

/**
 * A {@link PojoTypeIndexingPlanDelegate} that sends indexing events to an external queue,
 * to process them externally.
 *
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 */
final class PojoTypeIndexingPlanEventQueueDelegate<I, E> implements PojoTypeIndexingPlanDelegate<I, E> {

	private final PojoWorkTypeContext<I, E> typeContext;
	private final PojoWorkSessionContext sessionContext;
	private final PojoIndexingQueueEventSendingPlan sendingPlan;

	PojoTypeIndexingPlanEventQueueDelegate(PojoWorkTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext,
			PojoIndexingQueueEventSendingPlan sendingPlan) {
		this.typeContext = typeContext;
		this.sessionContext = sessionContext;
		this.sendingPlan = sendingPlan;
	}

	@Override
	public boolean isDirtyForAddOrUpdate(boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPathsOrNull) {
		// We will execute the addOrUpdate below
		// if the dirty paths require the entity itself OR a containing entity to be reindexed.
		// In both cases, we will send an event so that the reindexing is done in a background process.
		return forceSelfDirty || forceContainingDirty
				|| dirtyPathsOrNull != null && typeContext.reindexingResolver().dirtySelfOrContainingFilter().test( dirtyPathsOrNull );
	}

	@Override
	public void add(I identifier, DocumentRouteDescriptor route, Supplier<E> entitySupplier) {
		DirtinessDescriptor dirtiness = new DirtinessDescriptor(
				true, true,
				Collections.emptySet(),
				false
		);
		sendingPlan.append( typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				new PojoIndexingQueueEventPayload( DocumentRoutesDescriptor.of( route ), dirtiness ) );
	}

	@Override
	public void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths,
			boolean updatedBecauseOfContained, boolean updateBecauseOfDirty) {
		DirtinessDescriptor dirtiness = new DirtinessDescriptor(
				forceSelfDirty, forceContainingDirty,
				typeContext.pathOrdinals().toPathSet( dirtyPaths ),
				updatedBecauseOfContained
		);
		sendingPlan.append(
				typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				new PojoIndexingQueueEventPayload( routes, dirtiness )
		);
	}

	@Override
	public void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		// This is only relevant if, upon processing the delete event,
		// we load the entity and it's actually present.
		// In that case, we expect the entity itself to be reindexed.
		DirtinessDescriptor dirtiness = new DirtinessDescriptor(
				true, false,
				Collections.emptySet(),
				false
		);
		sendingPlan.append( typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				new PojoIndexingQueueEventPayload( routes, dirtiness ) );
	}

	@Override
	public void discard() {
		throw new AssertionFailure( "discard() should be handled at the strategy level" );
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory, OperationSubmitter operationSubmitter) {
		throw new AssertionFailure( "executeAndReport() should be handled at the strategy level" );
	}

}
