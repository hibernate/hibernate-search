/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

/**
 * A single implementation for all the bridge context interfaces that rely on the session context.
 * <p>
 * We could split it into one class per interfaces, but currently we simply do not need to,
 * since the only feature provided by each interface is an access to the extension.
 * This might change in the future, though, which is why the interfaces themselves are split.
 */
public final class SessionBasedBridgeOperationContext
		implements IdentifierBridgeFromDocumentIdentifierContext,
		RoutingBridgeRouteContext,
		TypeBridgeWriteContext,
		PropertyBridgeWriteContext,
		ValueBridgeFromIndexedValueContext {

	private final BridgeSessionContext sessionContext;

	public SessionBasedBridgeOperationContext(BridgeSessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	public <T> T extension(IdentifierBridgeFromDocumentIdentifierContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public String tenantIdentifier() {
		return sessionContext.tenantIdentifier();
	}

	@Override
	public Object tenantIdentifierValue() {
		return sessionContext.tenantIdentifierValue();
	}

	@Override
	public <T> T extension(RoutingBridgeRouteContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public <T> T extension(TypeBridgeWriteContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public <T> T extension(PropertyBridgeWriteContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public <T> T extension(ValueBridgeFromIndexedValueContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}
}
