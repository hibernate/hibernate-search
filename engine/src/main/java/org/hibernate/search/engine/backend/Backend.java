/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend;

import org.hibernate.search.util.common.SearchException;

/**
 * A backend as viewed by Hibernate Search users.
 * <p>
 * This interface exposes all operations that Hibernate Search users
 * should be able to execute directly on the backend, without having to go through mapper-specific APIs.
 */
public interface Backend {

	// TODO add standard APIs related to analysis (which is backend-scoped). To test if an analyzer is defined, for example.
	// TODO add standard APIs related to statistics?
	// TODO add other standard backend APIs?

	/**
	 * Unwrap the backend to some implementation-specific type.
	 *
	 * @param clazz The {@link Class} representing the expected type
	 * @param <T> The expected type
	 * @return The unwrapped backend.
	 * @throws SearchException if the backend implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clazz);

}
