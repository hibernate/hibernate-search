/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.SessionBasedBridgeOperationContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

public abstract class AbstractPojoSearchSession implements PojoWorkSessionContext, PojoScopeSessionContext {

	private final PojoSearchSessionMappingContext mappingContext;

	private final SessionBasedBridgeOperationContext sessionBasedBridgeOperationContext;

	protected AbstractPojoSearchSession(PojoSearchSessionMappingContext mappingContext) {
		this.mappingContext = mappingContext;
		this.sessionBasedBridgeOperationContext = new SessionBasedBridgeOperationContext( this );
	}

	@Override
	public PojoSearchSessionMappingContext mappingContext() {
		return mappingContext;
	}

	@Override
	public final IdentifierBridgeFromDocumentIdentifierContext identifierBridgeFromDocumentIdentifierContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public RoutingBridgeRouteContext routingBridgeRouteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final TypeBridgeWriteContext typeBridgeWriteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final PropertyBridgeWriteContext propertyBridgeWriteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final ValueBridgeFromIndexedValueContext valueBridgeFromIndexedValueContext() {
		return sessionBasedBridgeOperationContext;
	}

	protected PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

}
