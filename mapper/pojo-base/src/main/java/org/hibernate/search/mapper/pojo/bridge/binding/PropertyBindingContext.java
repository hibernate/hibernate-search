/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyIndexingDependencyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context provided to the {@link PropertyBinder#bind(PropertyBindingContext)} method.
 */
public interface PropertyBindingContext extends BindingContext {

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridge The bridge to use at runtime to convert between the POJO property and the index field value.
	 */
	default void bridge(PropertyBridge<Object> bridge) {
		bridge( Object.class, bridge );
	}

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	default void bridge(BeanHolder<? extends PropertyBridge<Object>> bridgeHolder) {
		bridge( Object.class, bridgeHolder );
	}

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param expectedPropertyType The type of the property expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridge The bridge to use at runtime to convert between the POJO property and the index field value.
	 * @param <P2> The type of the property expected by the given bridge.
	 */
	<P2> void bridge(Class<P2> expectedPropertyType, PropertyBridge<P2> bridge);

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param expectedPropertyType The type of the property expected by the given bridge.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param <P2> The type of the property expected by the given bridge.
	 */
	<P2> void bridge(Class<P2> expectedPropertyType, BeanHolder<? extends PropertyBridge<P2>> bridgeHolder);

	/**
	 * Sets the bridge implementing the property/index binding.
	 * Can be used if the property type of the bridged element is a {@link Map}.
	 *
	 * @param keyType The key type of the map property expected by the given bridge.
	 * @param valueType The value type of the map property expected by the given bridge.
	 * @param bridge The bridge to use at runtime to convert between the POJO property and the index field value.
	 * @param <K> The key type of the map property expected by the given bridge.
	 * @param <V> The value type of the map property expected by the given bridge.
	 */
	<K, V> void mapPropertyBridge(Class<K> keyType, Class<V> valueType, PropertyBridge<Map<K, V>> bridge);

	/**
	 * Sets the bridge implementing the property/index binding.
	 * Can be used if the property type of the bridged element is a {@link Map}.
	 *
	 * @param keyType The key type of the map property expected by the given bridge.
	 * @param valueType The value type of the map property expected by the given bridge.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param <K> The key type of the map property expected by the given bridge.
	 * @param <V> The value type of the map property expected by the given bridge.
	 */
	<K, V> void mapPropertyBridge(Class<K> keyType, Class<V> valueType,
			BeanHolder<? extends PropertyBridge<Map<K, V>>> bridgeHolder);

	/**
	 * Sets the bridge implementing the property/index binding.
	 * Can be used if the property type of the bridged element is a {@link List}.
	 *
	 * @param elementType The element type of the list property expected by the given bridge.
	 * @param bridge The bridge to use at runtime to convert between the POJO property and the index field value.
	 * @param <E> The element type of the list property expected by the given bridge.
	 */
	<E> void listPropertyBridge(Class<E> elementType, PropertyBridge<List<E>> bridge );

	/**
	 * Sets the bridge implementing the property/index binding.
	 * Can be used if the property type of the bridged element is a {@link List}.
	 *
	 * @param elementType The element type of the list property expected by the given bridge.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param <E> The element type of the list property expected by the given bridge.
	 */
	<E> void listPropertyBridge(Class<E> elementType,
			BeanHolder<? extends PropertyBridge<List<E>>> bridgeHolder );

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 */
	@Incubating
	PojoModelProperty bridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoPropertyIndexingDependencyConfigurationContext dependencies();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory typeFactory();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement indexSchemaElement();

}
