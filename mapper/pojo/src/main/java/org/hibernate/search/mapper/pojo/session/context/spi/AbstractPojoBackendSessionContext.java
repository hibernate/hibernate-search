/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.context.spi;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.BridgeSessionContext;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoBackendMappingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public abstract class AbstractPojoBackendSessionContext implements BackendSessionContext {

	private final BridgeSessionContext bridgeSessionContext;

	public AbstractPojoBackendSessionContext() {
		this.bridgeSessionContext = new BridgeSessionContext( this );
	}

	@Override
	public abstract AbstractPojoBackendMappingContext getMappingContext();

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

	public final ValueBridgeFromIndexedValueContext getValueBridgeFromIndexedValueContext() {
		return bridgeSessionContext;
	}
}
