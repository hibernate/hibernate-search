/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContextExtension;

/**
 * A bridge from a POJO entity to a document routing key.
 */
public interface RoutingKeyBridge extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given context,
	 * i.e. to an object type in the POJO model and to an element in the index schema.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to:
	 * <ul>
	 *     <li>Check the type of bridged elements
	 *     using {@link RoutingKeyBridgeBindingContext#getBridgedElement()}.
	 *     <li>Declare its dependencies, i.e. the properties
	 * 	   that will later be used in the
	 * 	   {@link #toRoutingKey(String, Object, Object, RoutingKeyBridgeToRoutingKeyContext)} method
	 * 	   using {@link RoutingKeyBridgeBindingContext#getDependencies()}.
	 *     <li>Optionally (advanced use) use reflection to retrieve accessors to the bridged element
	 *     that will later be used in the
	 *     {@link #toRoutingKey(String, Object, Object, RoutingKeyBridgeToRoutingKeyContext)} method
	 *     using {@link RoutingKeyBridgeBindingContext#getBridgedElement()}.
	 * </ul>
	 *
	 * @param context An entry point allowing to perform the operations listed above.
	 */
	void bind(RoutingKeyBridgeBindingContext context);

	/**
	 * Generate a routing key using the given {@code tenantIdentifier}, {@code entityIdentifier} and {@code bridgedElement}
	 * as input and transforming them as necessary.
	 * <p>
	 * <strong>Warning:</strong> Reading from {@code bridgedElement} should be done with care.
	 * Any read that was not declared in the {@link #bind(RoutingKeyBridgeBindingContext)} method
	 * (by declaring dependencies using {@link RoutingKeyBridgeBindingContext#getDependencies()}
	 * or (advanced use) creating an accessor using {@link RoutingKeyBridgeBindingContext#getBridgedElement()})
	 * may lead to out-of-sync indexes,
	 * because Hibernate Search will consider the read property irrelevant to indexing
	 * and will not reindex entities when that property changes.
	 *
	 * @param tenantIdentifier The tenant identifier currently in use ({@code null} if none).
	 * @param entityIdentifier The value of the POJO property used to generate the document identifier,
	 * i.e. the same value that was passed to {@link IdentifierBridge#toDocumentIdentifier(Object, org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext)}.
	 * @param bridgedElement The element this bridge is applied to, from which data should be read.
	 * @param context A context that can be
	 * {@link RoutingKeyBridgeToRoutingKeyContext#extension(RoutingKeyBridgeToRoutingKeyContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The resulting routing key. Never null.
	 */
	String toRoutingKey(String tenantIdentifier, Object entityIdentifier, Object bridgedElement,
			RoutingKeyBridgeToRoutingKeyContext context);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
