/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.model.PojoElement;

/**
 * A bridge from a POJO entity to a document routing key.
 *
 * @author Yoann Rodiere
 */
public interface RoutingKeyBridge extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given context,
	 * i.e. to an object type in the POJO model and to an element in the index schema.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Declare its expectations regarding the POJO model (input type, expected properties and their type, ...)
	 *     using {@link RoutingKeyBridgeBindingContext#getBridgedElement()}.
	 *     <li>Retrieve accessors to the POJO and to the index fields that will later be used in the
	 *     {@link #toRoutingKey(String, Object, PojoElement)} method
	 *     using {@link RoutingKeyBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 */
	void bind(RoutingKeyBridgeBindingContext context);

	/**
	 * Generate a routing key using the given {@code tenantIdentifier}, {@code entityIdentifier} and {@link PojoElement}
	 * as input and transforming them as necessary.
	 * <p>
	 * Reading from the {@link PojoElement} should be done using
	 * {@link org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor}s retrieved when the
	 * {@link #bind(RoutingKeyBridgeBindingContext)} method was called.
	 *
	 * @param tenantIdentifier The tenant identifier currently in use ({@code null} if none).
	 * @param entityIdentifier The value of the POJO property used to generate the document identifier,
	 * i.e. the same value that was passed to {@link IdentifierBridge#toDocumentIdentifier(Object)}.
	 * @param source The {@link PojoElement} to read from.
	 */
	String toRoutingKey(String tenantIdentifier, Object entityIdentifier, PojoElement source);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
