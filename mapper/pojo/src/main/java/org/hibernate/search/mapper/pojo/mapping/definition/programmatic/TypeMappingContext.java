/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeDefinition;
import org.hibernate.search.mapper.pojo.bridge.spi.RoutingKeyBridge;

/**
 * @author Yoann Rodiere
 */
public interface TypeMappingContext {

	TypeMappingContext indexed();

	TypeMappingContext indexed(String indexName);

	TypeMappingContext routingKeyBridge(String bridgeName);

	TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass);

	TypeMappingContext routingKeyBridge(String bridgeName, Class<? extends RoutingKeyBridge> bridgeClass);

	TypeMappingContext bridge(BridgeDefinition<?> definition);

	PropertyMappingContext property(String propertyName);

}
