/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.common.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.EntityReference;

public interface EntityReferenceFactory {

	/**
	 * @param typeName The name of the entity type.
	 * @param identifier The identifier of the entity.
	 * @return A reference to the entity.
	 * @throws RuntimeException If something goes wrong (exception while rendering an identifier, ...)
	 */
	EntityReference createEntityReference(String typeName, Object identifier);

	/**
	 * @param factory The factory for entity references.
	 * @param typeName The name of the entity type.
	 * @param identifier The identifier of the entity.
	 * @param exceptionSink A sink for exceptions thrown during the execution of this method.
	 * Any exception thrown while creating the entity reference should be {@link Consumer#accept(Object) put into}
	 * that sink and should not be propagated.
	 * @return A reference to the entity, or null if an exception was thrown while creating the entity reference.
	 */
	static EntityReference safeCreateEntityReference(EntityReferenceFactory factory, String typeName, Object identifier,
			Consumer<Exception> exceptionSink) {
		try {
			return factory.createEntityReference( typeName, identifier );
		}
		catch (RuntimeException e) {
			exceptionSink.accept( e );
			return null;
		}
	}

}
