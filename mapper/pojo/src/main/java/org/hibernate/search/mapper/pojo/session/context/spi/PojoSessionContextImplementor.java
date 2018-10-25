/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.context.spi;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.BridgeSessionContextImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public abstract class PojoSessionContextImplementor implements SessionContextImplementor {

	private final BridgeSessionContextImpl bridgeSessionContext;

	public PojoSessionContextImplementor() {
		this.bridgeSessionContext = new BridgeSessionContextImpl( this );
	}

	public abstract PojoRuntimeIntrospector getRuntimeIntrospector();

	public final IdentifierBridgeFromDocumentIdentifierContext getIdentifierBridgeFromDocumentIdentifierContext() {
		return bridgeSessionContext;
	}

	public final RoutingKeyBridgeToRoutingKeyContext getRoutingKeyBridgeToRoutingKeyContext() {
		return bridgeSessionContext;
	}

	public final TypeBridgeWriteContext getTypeBridgeWriteContext() {
		return bridgeSessionContext;
	}

	public final PropertyBridgeWriteContext getPropertyBridgeWriteContext() {
		return bridgeSessionContext;
	}
}
