/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;


import org.hibernate.search.engine.mapper.session.context.SessionContext;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBridgeBindingContext;

/**
 * A bridge between a POJO property of type {@code I} and a document identifier.
 *
 * @author Yoann Rodiere
 */
public interface IdentifierBridge<I> extends AutoCloseable {

	/**
	 * Bind this bridge instance to the given context,
	 * i.e. to an object property in the POJO model.
	 * <p>
	 * This method is called exactly once for each bridge instance, before any other method.
	 * It allows the bridge to inspect the type of values extracted from the POJO model that will be passed to this bridge.
	 *
	 * @param context An entry point to perform the operations listed above.
	 */
	default void bind(IdentifierBridgeBindingContext<I> context) {
		// No-op by default
	}

	/**
	 * Transform the given POJO property value to the value of the document identifier.
	 * <p>
	 * Must return a unique value for each value of {@code propertyValue}
	 *
	 * @param propertyValue The POJO property value to be transformed.
	 * @return The value of the document identifier.
	 */
	String toDocumentIdentifier(I propertyValue);

	/**
	 * Transform the given document identifier value back to the value of the POJO property.
	 * <p>
	 * Must be the exact inverse function of {@link #toDocumentIdentifier(Object)},
	 * i.e. {@code object.equals(fromDocumentIdentifier(toDocumentIdentifier(object, sessionContext)))}
	 * must always be true.
	 *
	 * @param documentIdentifier The document identifier value to be transformed.
	 * @param sessionContext A {@link SessionContext} that can be {@link SessionContext#unwrap(Class) unwrapped}
	 * to a more useful type, such as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The value of the document identifier.
	 */
	I fromDocumentIdentifier(String documentIdentifier, SessionContext sessionContext);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
