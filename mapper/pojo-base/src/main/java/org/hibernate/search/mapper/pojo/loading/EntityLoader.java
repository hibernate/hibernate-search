/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading;

import java.util.List;

/**
 * A start loader for entity loading entities during mass indexing.
 *
 * @param <E> The resulting entity type (output)
 */
public interface EntityLoader<E> extends AutoCloseable {

	/**
	 * Loads the entities corresponding to the given identifiers, blocking the current thread while doing so.
	 *
	 * @param identifiers A list of identifiers for objects to load.
	 * @return A list of entities, in the same order the identifiers were given.
	 * {@code null} is inserted when an object is not found or has the wrong concrete type.
	 */
	List<E> load(List<?> identifiers);

	/**
	 * Closes this resource, relinquishing any underlying resources.
	 * This method is invoked automatically on objects managed by the
	 */
	@Override
	default void close() {
		//no-op
	}
}
