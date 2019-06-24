/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyDependencyContext;

/**
 * The context provided to the {@link PropertyBridge#bind(PropertyBridgeBindingContext)} method.
 */
public interface PropertyBridgeBindingContext {

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 * @hsearch.experimental This feature is under active development.
	 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
	 */
	PojoModelProperty getBridgedElement();

	/**
	 * @return An entry point allowing to declare the parts of the entity graph that this bridge will depend on.
	 */
	PojoPropertyDependencyContext getDependencies();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactory getTypeFactory();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement getIndexSchemaElement();

}
