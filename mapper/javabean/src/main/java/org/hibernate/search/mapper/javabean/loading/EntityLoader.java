/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading;

import java.util.List;

public interface EntityLoader<E> {

	/**
	 * Loads the entities corresponding to the given identifiers, blocking the current thread while doing so.
	 *
	 * @param identifiers A list of identifiers for objects to load.
	 * @return A list of entities, in the same order the identifiers were given.
	 * {@code null} is inserted when an object is not found or has the wrong concrete type.
	 */
	List<E> load(List<?> identifiers);

}
