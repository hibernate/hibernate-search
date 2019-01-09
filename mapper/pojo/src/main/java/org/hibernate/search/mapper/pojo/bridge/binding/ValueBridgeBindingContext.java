/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;

/**
 * The context provided to the {@link ValueBridge#bind(ValueBridgeBindingContext)} method.
 */
public interface ValueBridgeBindingContext<V> {

	/**
	 * @return An entry point allowing to inspect the type of values that will be passed to this bridge.
	 */
	PojoModelValue<V> getBridgedElement();

	/**
	 * @return An entry point allowing to define a new field type.
	 */
	IndexFieldTypeFactoryContext getTypeFactory();

}
