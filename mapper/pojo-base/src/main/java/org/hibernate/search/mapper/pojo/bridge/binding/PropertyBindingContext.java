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
	void setBridge(PropertyBridge bridge);

	/**
	 * Sets the bridge implementing the property/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the POJO property and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to PropertyBridge, however.
	void setBridge(BeanHolder<? extends PropertyBridge> bridgeHolder);

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 */
	@Incubating
	PojoModelProperty getBridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoPropertyIndexingDependencyConfigurationContext getDependencies();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory getTypeFactory();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement getIndexSchemaElement();

}
