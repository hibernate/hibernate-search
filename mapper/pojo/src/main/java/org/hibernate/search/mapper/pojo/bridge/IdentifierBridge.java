/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;


/**
 * A bridge between a POJO property of type {@code T} and a document identifier.
 *
 * @author Yoann Rodiere
 */
public interface IdentifierBridge<T> extends AutoCloseable {

	/**
	 * Transform the given POJO property value to the value of the document identifier.
	 * <p>
	 * Must return a unique value for each value of {@code propertyValue}
	 *
	 * @param propertyValue The POJO property value to be transformed.
	 * @return The value of the document identifier.
	 */
	String toDocumentIdentifier(T propertyValue);

	/**
	 * Transform the given document identifier value back to the value of the POJO property.
	 * <p>
	 * Must be the exact inverse function of {@link #toDocumentIdentifier(Object)},
	 * i.e. {@code object.equals(fromDocumentIdentifier(toDocumentIdentifier(object)))} must always be true.
	 *
	 * @param documentIdentifier The document identifier value to be transformed.
	 * @return The value of the document identifier.
	 */
	T fromDocumentIdentifier(String documentIdentifier);

	/**
	 * Close any resource before the bridge is discarded.
	 */
	@Override
	default void close() {
	}

}
