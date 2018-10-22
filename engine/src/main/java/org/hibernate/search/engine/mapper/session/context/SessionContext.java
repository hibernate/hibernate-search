/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.session.context;

/**
 * The session context, provided by the mapper.
 * <p>
 * Depending on the mapper being used, this may give access, through {@link #unwrap(Class)},
 * to different objects, such as a Hibernate ORM Session.
 */
public interface SessionContext {

	/**
	 * Unwrap the session context to some implementation-specific type.
	 *
	 * @param clazz The {@link Class} representing the expected type.
	 * @param <T> The expected type.
	 * @return The unwrapped session context.
	 * @throws org.hibernate.search.util.SearchException if the session context implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clazz);

}
