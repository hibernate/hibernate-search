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
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * A context to map a type to the index schema.
 */
public interface TypeMappingContext {

	/**
	 * Maps an entity type to an index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 */
	TypeMappingContext indexed();

	/**
	 * Maps an entity type to an index.
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 * @see Indexed#index()
	 */
	TypeMappingContext indexed(String indexName);

	/**
	 * Maps an entity type to an index.
	 * @param backendName The name of the backend.
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 * @see Indexed#backend()
	 * @see Indexed#index()
	 */
	TypeMappingContext indexed(String backendName, String indexName);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBridge
	 */
	TypeMappingContext routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBridge
	 */
	TypeMappingContext routingKeyBridge(BeanReference<? extends RoutingKeyBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBridge
	 */
	TypeMappingContext routingKeyBridge(BridgeBuilder<? extends RoutingKeyBridge> builder);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see TypeBridge
	 */
	TypeMappingContext bridge(Class<? extends TypeBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see TypeBridge
	 */
	TypeMappingContext bridge(BeanReference<? extends TypeBridge> bridgeReference);

	/**
	 * @param builder A bridge builder.
	 * @return {@code this}, for method chaining.
	 * @see TypeBridge
	 */
	TypeMappingContext bridge(BridgeBuilder<? extends TypeBridge> builder);

	/**
	 * Starts the definition of the mapping of a specific property.
	 *
	 * @param propertyName The name of a property in this type.
	 * @return A context to map this property.
	 */
	PropertyMappingContext property(String propertyName);

}
