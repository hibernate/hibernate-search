/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContextExtension;
import org.hibernate.search.mapper.pojo.model.PojoElement;

/**
 * A bridge between a POJO property and an element of the index schema.
 * <p>
 * The {@code PropertyBridge} interface is a more powerful version of {@link ValueBridge}
 * that can use reflection to get information about the property being bridged,
 * and can contribute more than one index field, in particular.
 *
 * @author Yoann Rodiere
 */
public interface PropertyBridge extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given context,
	 * i.e. to an object property in the POJO model and to an element in the index schema.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the index schema (field names and field types, storage options, ...)
	 *     and retrieve references to the index fields that will later be used in the
	 * 	   {@link #write(DocumentElement, PojoElement, PropertyBridgeWriteContext)} method
	 *     using {@link PropertyBridgeBindingContext#getIndexSchemaElement()}.
	 *     <li>Check the type of bridged elements
	 *     using {@link PropertyBridgeBindingContext#getBridgedElement()}.
	 *     <li>Declare its dependencies, i.e. the properties
	 * 	   that will later be used in the
	 *     {@link #write(DocumentElement, PojoElement, PropertyBridgeWriteContext)} method
	 * 	   using {@link PropertyBridgeBindingContext#getDependencies()}.
	 *     <li>Optionally (advanced use) use reflection to retrieve accessors to the bridged element
	 *     that will later be used in the
	 *     {@link #write(DocumentElement, PojoElement, PropertyBridgeWriteContext)} method
	 *     using {@link PropertyBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 */
	void bind(PropertyBridgeBindingContext context);

	/**
	 * Write to fields in the given {@link DocumentElement},
	 * using the given {@link PojoElement} as input and transforming it as necessary.
	 * <p>
	 * Writing to the {@link DocumentElement} should be done using
	 * {@link org.hibernate.search.engine.backend.document.IndexFieldReference}s retrieved when the
	 * {@link #bind(PropertyBridgeBindingContext)} method was called.
	 * <p>
	 * Reading from the {@link PojoElement} should be done using
	 * {@link org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor}s retrieved when the
	 * {@link #bind(PropertyBridgeBindingContext)} method was called.
	 * @param target The {@link DocumentElement} to write to.
	 * @param source The {@link PojoElement} to read from.
	 * @param context A context that can be
	 * {@link PropertyBridgeWriteContext#extension(PropertyBridgeWriteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 */
	void write(DocumentElement target, PojoElement source, PropertyBridgeWriteContext context);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
