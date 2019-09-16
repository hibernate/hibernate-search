/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;

/**
 * A single implementation for all the bridge context interfaces that rely on the session context.
 * <p>
 * We could split it into one class per interfaces, but currently we simply do not need to,
 * since the only feature provided by each interface is an access to the extension.
 * This might change in the future, though, which is why the interfaces themselves are split.
 */
public final class BridgeSessionContext
		implements IdentifierBridgeFromDocumentIdentifierContext,
				RoutingKeyBridgeToRoutingKeyContext,
				TypeBridgeWriteContext,
				PropertyBridgeWriteContext,
				ValueBridgeFromIndexedValueContext {

	private final BackendSessionContext sessionContext;

	public BridgeSessionContext(BackendSessionContext sessionContext) {
		this.sessionContext = sessionContext;
	}

	@Override
	public <T> T extension(IdentifierBridgeFromDocumentIdentifierContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this, sessionContext ) );
	}

	@Override
	public <T> T extension(RoutingKeyBridgeToRoutingKeyContextExtension<T> extension) {
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
