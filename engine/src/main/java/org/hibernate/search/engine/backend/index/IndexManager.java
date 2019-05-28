/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.index;

import org.hibernate.search.util.common.SearchException;

/**
 * An index manager as viewed by Hibernate Search users.
 * <p>
 * This interface exposes all operations that Hibernate Search users
 * should be able to execute directly on the index manager, without having to go through mapper-specific APIs.
 */
public interface IndexManager {

	// TODO add standard APIs related to statistics?

	/**
	 * Unwrap the index manager to some implementation-specific type.
	 *
	 * @param clazz The {@link Class} representing the expected type
	 * @param <T> The expected type
	 * @return The unwrapped index manager.
	 * @throws SearchException if the index manager implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clazz);

}
