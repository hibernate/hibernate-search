/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierMappingImplementor;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class PojoIndexingQueueEventProcessingPlanImpl implements PojoIndexingQueueEventProcessingPlan {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkIndexedTypeContextProvider typeContextProvider;
	private final PojoWorkSessionContext sessionContext;
	private final PojoIndexingPlan delegate;

	public PojoIndexingQueueEventProcessingPlanImpl(PojoWorkIndexedTypeContextProvider typeContextProvider,
			PojoWorkSessionContext sessionContext, PojoIndexingPlan delegate) {
		this.typeContextProvider = typeContextProvider;
		this.sessionContext = sessionContext;
		this.delegate = delegate;
	}

	@Override
	public void add(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		PojoWorkIndexedTypeContext<?, ?> typeContext = typeContext( entityName );
		Object id = typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
		delegate.add( typeContext.typeIdentifier(), id, routes, null );
	}

	@Override
	public void addOrUpdate(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		PojoWorkIndexedTypeContext<?, ?> typeContext = typeContext( entityName );
		Object id = typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
		delegate.addOrUpdate( typeContext.typeIdentifier(), id, routes, null );
	}

	@Override
	public void delete(String entityName, String serializedId, DocumentRoutesDescriptor routes) {
		PojoWorkIndexedTypeContext<?, ?> typeContext = typeContext( entityName );
		Object id = typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
		delegate.delete( typeContext.typeIdentifier(), id, routes, null );
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return delegate.executeAndReport( entityReferenceFactory );
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
		PojoWorkIndexedTypeContext<?, ?> typeContext = typeContext( entityName );
		return typeContext.identifierMapping().fromDocumentIdentifier( serializedId, sessionContext );
	}

	private PojoWorkIndexedTypeContext<?, ?> typeContext(String entityName) {
		Optional<? extends PojoWorkIndexedTypeContext<?, ?>> optional = typeContextProvider.forEntityName( entityName );
		if ( !optional.isPresent() ) {
			throw log.nonIndexedTypeInIndexingEvent( entityName );
		}
		return optional.get();
	}
}
