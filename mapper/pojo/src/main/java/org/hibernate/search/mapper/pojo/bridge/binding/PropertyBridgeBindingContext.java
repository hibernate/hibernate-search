/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

/**
 * The context provided to the {@link PropertyBridge#bind(PropertyBridgeBindingContext)} method.
 */
public interface PropertyBridgeBindingContext {

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the bridged POJO property.
	 */
	PojoModelProperty getBridgedElement();

	/**
	 * @return An entry point allowing to declare expectations and retrieve accessors to the index schema.
	 */
	IndexSchemaElement getIndexSchemaElement();

}
