/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContextExtension;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContextExtension;

/**
 * A bridge between a POJO property of type {@code I} and a document identifier.
 *
 * @param <I> The type of identifiers on the POJO side of the bridge.
 */
public interface IdentifierBridge<I> extends AutoCloseable {

	/**
	 * Transform the given POJO property value to the value of the document identifier.
	 * <p>
	 * Must return a unique value for each value of {@code propertyValue}
	 *
	 * @param propertyValue The POJO property value to be transformed.
	 * @param context A context that can be
	 * {@link IdentifierBridgeToDocumentIdentifierContext#extension(IdentifierBridgeToDocumentIdentifierContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM SessionFactory (if using the Hibernate ORM mapper).
	 * @return The value of the document identifier.
	 */
	String toDocumentIdentifier(I propertyValue, IdentifierBridgeToDocumentIdentifierContext context);

	/**
	 * Transform the given document identifier value back to the value of the POJO property.
	 * <p>
	 * Must be the exact inverse function of {@link #toDocumentIdentifier(Object, IdentifierBridgeToDocumentIdentifierContext)},
	 * i.e. {@code object.equals(fromDocumentIdentifier(toDocumentIdentifier(object, sessionContext)))}
	 * must always be true.
	 *
	 * @param documentIdentifier The document identifier value to be transformed.
	 * @param context A sessionContext that can be
	 * {@link IdentifierBridgeFromDocumentIdentifierContext#extension(IdentifierBridgeFromDocumentIdentifierContextExtension) extended}
	 * to a more useful type, giving access to such things as a Hibernate ORM Session (if using the Hibernate ORM mapper).
	 * @return The value of the document identifier.
	 */
	I fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context);

	default boolean isCompatibleWith(IdentifierBridge<?> other) {
		return equals( other );
	}

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
