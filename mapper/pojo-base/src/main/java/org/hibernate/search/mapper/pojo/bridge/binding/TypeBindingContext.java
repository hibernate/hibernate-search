/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context provided to the {@link TypeBinder#bind(TypeBindingContext)} method.
 */
public interface TypeBindingContext extends BindingContext {

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param bridge The bridge to use at runtime to convert between the type and the index field value.
	 */
	default void bridge(TypeBridge<Object> bridge) {
		bridge( Object.class, bridge );
	}

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the type and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	default void bridge(BeanHolder<? extends TypeBridge<Object>> bridgeHolder) {
		bridge( Object.class, bridgeHolder );
	}

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param expectedEntityType The type of the entity expected by the given bridge.
	 * @param bridge The bridge to use at runtime to convert between the type and the index field value.
	 * @param <T2> The type of bridged elements expected by the given bridge.
	 */
	<T2> void bridge(Class<T2> expectedEntityType, TypeBridge<T2> bridge);

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param expectedEntityType The type of the entity expected by the given bridge.
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the type and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @param <T2> The type of bridged elements expected by the given bridge.
	 */
	<T2> void bridge(Class<T2> expectedEntityType, BeanHolder<? extends TypeBridge<T2>> bridgeHolder);

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type.
	 */
	@Incubating
	PojoModelType bridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoTypeIndexingDependencyConfigurationContext dependencies();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory typeFactory();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement indexSchemaElement();

}
