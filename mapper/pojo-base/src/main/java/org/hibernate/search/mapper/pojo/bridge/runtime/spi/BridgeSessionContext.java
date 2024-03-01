/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContextExtension;

/**
 * Session-scoped information and operations for use in bridges.
 * <p>
 * Used in particular when extending contexts:
 * {@link ValueBridgeFromIndexedValueContextExtension#extendOptional(ValueBridgeFromIndexedValueContext, BridgeSessionContext)},
 * ...
 */
public interface BridgeSessionContext {

	BridgeMappingContext mappingContext();

	String tenantIdentifier();

	Object tenantIdentifierValue();

	IdentifierBridgeFromDocumentIdentifierContext identifierBridgeFromDocumentIdentifierContext();

	RoutingBridgeRouteContext routingBridgeRouteContext();

	TypeBridgeWriteContext typeBridgeWriteContext();

	PropertyBridgeWriteContext propertyBridgeWriteContext();

	ValueBridgeFromIndexedValueContext valueBridgeFromIndexedValueContext();

}
