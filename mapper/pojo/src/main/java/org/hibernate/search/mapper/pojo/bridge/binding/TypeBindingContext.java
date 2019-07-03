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
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;

/**
 * The context provided to the {@link TypeBridgeBuilder#bind(TypeBindingContext)} method.
 */
public interface TypeBindingContext extends BindingContext {

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param bridge The bridge to use at runtime to convert between the type and the index field value.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to TypeBridge, however.
	void setBridge(TypeBridge bridge);

	/**
	 * Sets the bridge implementing the type/index binding.
	 *
	 * @param bridgeHolder A {@link BeanHolder} containing
	 * the bridge to use at runtime to convert between the type and the index field value.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	// FIXME also require the caller to pass the expected raw type here, and validate it.
	//  We'll need to add generic type parameters to TypeBridge, however.
	void setBridge(BeanHolder<? extends TypeBridge> bridgeHolder);

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO type.
	 * @hsearch.experimental This feature is under active development.
	 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
	 */
	PojoModelType getBridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoTypeIndexingDependencyConfigurationContext getDependencies();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory getTypeFactory();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement getIndexSchemaElement();

}
