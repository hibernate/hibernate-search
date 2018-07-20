/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge;

import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelType;

/**
 * A bridge from a POJO entity to a document routing key.
 *
 * @author Yoann Rodiere
 */
public interface RoutingKeyBridge extends AutoCloseable {

	void bind(PojoModelType pojoModelType);

	/**
	 * Generate a routing key using the given {@code tenantIdentifier}, {@code entityIdentifier} and {@link PojoElement}
	 * as input and transforming them as necessary.
	 * <p>
	 * Reading from the {@link PojoElement} should be done using
	 * {@link org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor}s retrieved when the
	 * {@link #bind(PojoModelType)} method was called.
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
