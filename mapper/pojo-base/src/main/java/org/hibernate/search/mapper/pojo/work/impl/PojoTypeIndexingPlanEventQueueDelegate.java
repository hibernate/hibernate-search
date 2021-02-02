/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventSendingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * A {@link PojoTypeIndexingPlanDelegate} that sends indexing events to an external queue,
 * to process them externally.
 *
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 */
final class PojoTypeIndexingPlanEventQueueDelegate<I, E> implements PojoTypeIndexingPlanDelegate<I, E> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final PojoWorkSessionContext sessionContext;
	private final PojoIndexingQueueEventSendingPlan delegate;

	PojoTypeIndexingPlanEventQueueDelegate(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext,
			PojoIndexingQueueEventSendingPlan delegate) {
		this.typeContext = typeContext;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public void add(I identifier, Supplier<E> entitySupplier, DocumentRouteDescriptor route) {
		delegate.add( typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				DocumentRoutesDescriptor.of( route ) );
	}

	@Override
	public void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		delegate.addOrUpdate( typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				routes );
	}

	@Override
	public void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		delegate.delete( typeContext.entityName(), identifier,
				typeContext.identifierMapping().toDocumentIdentifier( identifier, sessionContext.mappingContext() ),
				routes );
	}

	@Override
	public void discard() {
		delegate.discard();
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return delegate.sendAndReport( entityReferenceFactory );
	}

}
