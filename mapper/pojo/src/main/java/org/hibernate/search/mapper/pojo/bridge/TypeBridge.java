/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContextExtension;

/**
 * A bridge between a POJO type and an element of the index schema.
 * <p>
 * The {@code TypeBridge} interface is a more powerful version of {@link ValueBridge}
 * that applies to a whole type instead of a single property,
 * and can contribute more than one index field, in particular.
 */
public interface TypeBridge extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given context,
	 * i.e. to an object type in the POJO model and to an element in the index schema.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the index schema (field names and field types, storage options, ...)
	 *     and retrieve references to the index fields that will later be used in the
	 * 	   {@link #write(DocumentElement, Object, TypeBridgeWriteContext)} method
	 *     using {@link TypeBridgeBindingContext#getIndexSchemaElement()}.
	 *     <li>Check the type of bridged elements
	 *     using {@link TypeBridgeBindingContext#getBridgedElement()}.
	 *     <li>Declare its dependencies, i.e. the properties
	 * 	   that will later be used in the
	 *     {@link #write(DocumentElement, Object, TypeBridgeWriteContext)} method
	 * 	   using {@link TypeBridgeBindingContext#getDependencies()}.
	 *     <li>Optionally (advanced use) use reflection to retrieve accessors to the bridged element
	 *     that will later be used in the
	 *     {@link #write(DocumentElement, Object, TypeBridgeWriteContext)} method
	 *     using {@link TypeBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 */
	void bind(TypeBridgeBindingContext context);

	/**
	 * Write to fields in the given {@link DocumentElement},
	 * using the given {@code bridgedElement} as input and transforming it as necessary.
	 * <p>
	 * Writing to the {@link DocumentElement} should be done using
	 * {@link org.hibernate.search.engine.backend.document.IndexFieldReference}s retrieved when the
	 * {@link #bind(TypeBridgeBindingContext)} method was called.
	 * <p>
	 * <strong>Warning:</strong> Reading from {@code bridgedElement} should be done with care.
	 * Any read that was not declared in the {@link #bind(TypeBridgeBindingContext)} method
	 * (by declaring dependencies using {@link TypeBridgeBindingContext#getDependencies()}
	 * or (advanced use) creating an accessor using {@link TypeBridgeBindingContext#getBridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 *
	 * @param target The {@link DocumentElement} to write to.
	 * @param bridgedElement The element this bridge is applied to, from which data should be read.
	 * @param context A context that can be
	 * {@link TypeBridgeWriteContext#extension(TypeBridgeWriteContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 */
	void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
