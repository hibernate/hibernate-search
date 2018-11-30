/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * @author Yoann Rodiere
 */
public interface TypeMappingContext {

	TypeMappingContext indexed();

	TypeMappingContext indexed(String indexName);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext routingKeyBridge(BeanReference<? extends RoutingKeyBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext bridge(Class<? extends TypeBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext bridge(BeanReference<? extends TypeBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 */
	TypeMappingContext bridge(BridgeBuilder<? extends TypeBridge> builder);

	PropertyMappingContext property(String propertyName);

}
