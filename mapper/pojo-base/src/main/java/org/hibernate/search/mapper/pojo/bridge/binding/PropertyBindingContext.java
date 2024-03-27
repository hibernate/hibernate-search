/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

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
