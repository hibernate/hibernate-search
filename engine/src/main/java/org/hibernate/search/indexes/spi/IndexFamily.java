/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

/**
 * An IndexFamily abstracts the specific configuration and implementations
 * being used on a "family" of indexes, i.e. all the indexes using the same underlying
 * {@link IndexManagerType indexing technology}.
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
