/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.identity.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.work.spi.DirtinessDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventPayload;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public final class PojoIndexingQueueEventProcessingPlanImpl implements PojoIndexingQueueEventProcessingPlan {

	private final PojoWorkTypeContextProvider typeContextProvider;
	private final PojoWorkSessionContext sessionContext;
	private final PojoIndexingPlan delegate;

	public PojoIndexingQueueEventProcessingPlanImpl(PojoWorkTypeContextProvider typeContextProvider,
			PojoWorkSessionContext sessionContext, PojoIndexingPlan delegate) {
		this.typeContextProvider = typeContextProvider;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public void append(String entityName, String serializedId, PojoIndexingQueueEventPayload payload) {
		PojoWorkTypeContext<?, ?> typeContext = typeContext( entityName );
		Object id = typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
		DirtinessDescriptor dirtiness = payload.dirtiness;
		delegate.addOrUpdateOrDelete( typeContext.typeIdentifier(), id, payload.routes,
				// Force the reindexing now if the entity was marked as dirty because of a contained entity;
				// this is to avoid sending events forever and to force the processing of "updateBecauseOfContained" now.
				// See org.hibernate.search.mapper.pojo.work.impl.PojoTypeIndexingPlanIndexOrEventQueueDelegate.addOrUpdate
				dirtiness.updatedBecauseOfContained() || dirtiness.forceSelfDirty(),
				dirtiness.forceContainingDirty(),
				typeContext.pathOrdinals().toPathSelection( dirtiness.dirtyPaths() )
		);
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory, OperationSubmitter operationSubmitter) {
		return delegate.executeAndReport( entityReferenceFactory, operationSubmitter );
	}

	@Override
	public <I> String toSerializedId(String entityName, I identifier) {
		@SuppressWarnings("unchecked") // the provided identifier is supposed to have the right type
		IdentifierMappingImplementor<I, ?> identifierMapping =
				(IdentifierMappingImplementor<I, ?>) typeContext( entityName ).identifierMapping();
		return identifierMapping.toDocumentIdentifier( identifier, sessionContext.mappingContext() );
	}

	@Override
	public Object toIdentifier(String entityName, String serializedId) {
		PojoWorkTypeContext<?, ?> typeContext = typeContext( entityName );
		return typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
	}

	private PojoWorkTypeContext<?, ?> typeContext(String entityName) {
		return typeContextProvider.byEntityName().getOrFail( entityName );
	}
}
