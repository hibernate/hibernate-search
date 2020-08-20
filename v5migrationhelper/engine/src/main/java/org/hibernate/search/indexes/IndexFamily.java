/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes;

/**
 * An {@code IndexFamily} represents a set of indexes using the same {@link IndexFamilyType indexing technology}
 * and the same "global" configuration (e.g. the host for the Elasticsearch).
 * <p>
 * It actually contains information derived from the configuration and mapping,
 * thus it cannot be accessed directly through static methods.
 * However, it can be retrieved through the SearchFactory by passing an {@link IndexFamilyType} as a key.
 * <p>
 * As it happens, in Search 5, it is not possible to have multiple sets of "global" configuration per technology;
 * thus there will only ever be one instance of {@code IndexFamily} per technology,
 * i.e. there will always be a 1-1 mapping between {@link IndexFamilyType} and {@code IndexFamily}.
 * <p>
 * In future versions of Hibernate Search, we plan to allow multiple configurations per technology,
 * thus this 1-1 mapping may not hold.
 * <p>
 * Ideally this class would be called a "Backend" or "BackendManager",
 * but in Search 5 the term "backend" already means something subtly different,
 * related to the routing of works (locally, through JGroups, ...).
 *
 * @hsearch.experimental This type is under active development as part of the Elasticsearch integration.
 *    You should be prepared for incompatible changes in future releases.
 */
public interface IndexFamily {

	/**
	 * Unwrap the index family to some implementation-specific type.
	 *
	 * @param unwrappedClass The {@link Class} representing the expected index family type
	 * @return The unwrapped index family.
	 * @throws org.hibernate.search.exception.SearchException if the index family implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> unwrappedClass);

}
