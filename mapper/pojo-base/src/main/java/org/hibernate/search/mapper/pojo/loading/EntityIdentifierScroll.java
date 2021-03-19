/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading;

import java.util.List;

/**
 * A scroll identifiers of entities during mass indexing.
 *
 */
public interface EntityIdentifierScroll extends AutoCloseable {

	/**
	 * Get total count of indexing process.
	 *
	 * @return identifier total count.
	 */
	long totalCount();

	/**
	 * Get identifiers to indexing process.
	 *
	 * @return list of next identifiers.
	 * Returns null when it's out of elements.
	 */
	List<?> next();

	/**
	 * Closes this {@link EntityIdentifierScroll}.
	 */
	@Override
	default void close() {
		//no-op
	}

}
