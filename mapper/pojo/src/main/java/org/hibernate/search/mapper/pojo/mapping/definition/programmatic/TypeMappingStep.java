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
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

/**
 * The step in a mapping definition where a type can be mapped,
 * and where properties of that type can be referenced to map them more precisely.
 */
public interface TypeMappingStep {

	/**
	 * Maps an entity type to an index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 */
	TypeMappingStep indexed();

	/**
	 * Maps an entity type to an index.
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 * @see Indexed#index()
	 */
	TypeMappingStep indexed(String indexName);

	/**
	 * Maps an entity type to an index.
	 * @param backendName The name of the backend.
	 * @param indexName The name of the index.
	 * @return {@code this}, for method chaining.
	 * @see Indexed
	 * @see Indexed#backend()
	 * @see Indexed#index()
	 */
	TypeMappingStep indexed(String backendName, String indexName);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBridge
	 */
	TypeMappingStep routingKeyBridge(Class<? extends RoutingKeyBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBridge
	 */
	TypeMappingStep routingKeyBridge(BeanReference<? extends RoutingKeyBridge> bridgeReference);

	/**
	 * @param binder A {@link RoutingKeyBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see RoutingKeyBinder
	 */
	TypeMappingStep routingKeyBinder(RoutingKeyBinder<?> binder);

	/**
	 * @param bridgeClass The class of the bridge to use.
	 * @return {@code this}, for method chaining.
	 * @see TypeBridge
	 */
	TypeMappingStep bridge(Class<? extends TypeBridge> bridgeClass);

	/**
	 * @param bridgeReference A {@link BeanReference} pointing to the bridge to use.
	 * See the static "ofXXX()" methods of {@link BeanReference} for details about the various type of references
	 * (by name, by type, ...).
	 * @return {@code this}, for method chaining.
	 * @see TypeBridge
	 */
	TypeMappingStep bridge(BeanReference<? extends TypeBridge> bridgeReference);

	/**
	 * @param binder A {@link TypeBinder} responsible for creating a bridge.
	 * @return {@code this}, for method chaining.
	 * @see TypeBinder
	 */
	TypeMappingStep binder(TypeBinder<?> binder);

	/**
	 * Starts the definition of the mapping of a specific property.
	 *
	 * @param propertyName The name of a property in this type.
	 * @return A DSL step where the property mapping can be defined in more details.
	 */
	PropertyMappingStep property(String propertyName);

}
