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
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to PropertyBridge, however.
	void bridge(PropertyBridge bridge);

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridge The bridge to use at runtime to convert between the POJO property and the index field value.
	 * @deprecated Use {@link #bridge(PropertyBridge)} instead.
	 */
	@Deprecated
	default void setBridge(PropertyBridge bridge) {
		bridge( bridge );
	}

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to PropertyBridge, however.
	void bridge(BeanHolder<? extends PropertyBridge> bridgeHolder);

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 * @deprecated Use {@link #bridge(BeanHolder)} instead.
	 */
	@Deprecated
	default void setBridge(BeanHolder<? extends PropertyBridge> bridgeHolder) {
		bridge( bridgeHolder );
	}

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 */
	@Incubating
	PojoModelProperty bridgedElement();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 * @deprecated Use {@link #bridgedElement()} instead.
	 */
	@Deprecated
	default PojoModelProperty getBridgedElement() {
		return bridgedElement();
	}

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoPropertyIndexingDependencyConfigurationContext dependencies();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 * @deprecated Use {@link #dependencies()} instead.
	 */
	@Deprecated
	default PojoPropertyIndexingDependencyConfigurationContext getDependencies() {
		return dependencies();
	}

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory typeFactory();

	/**
	 * @return An entry point allowing to define a new field type.
	 * @deprecated Use {@link #typeFactory()} instead.
	 */
	@Deprecated
	default IndexFieldTypeFactory getTypeFactory() {
		return typeFactory();
	}

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement indexSchemaElement();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 * @deprecated Use {@link #indexSchemaElement()} instead.
	 */
	@Deprecated
	default IndexSchemaElement getIndexSchemaElement() {
		return indexSchemaElement();
	}

}
