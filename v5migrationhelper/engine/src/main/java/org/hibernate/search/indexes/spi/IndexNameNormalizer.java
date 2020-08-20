/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

/**
 * Allows the caller of an {@link IndexManager} to get the actual name of the index in the
 * backend.
 *
 * @author Davide D'Alto
 */
public interface IndexNameNormalizer {

	/**
	 * Some index managers need to normalize the name of the index before using it with the backend,
	 * this method will return the actual name used by the index manager.
	 * <p>
	 * Elasticsearch, for example, will lowercase the name giving space to possible conflicts.
	 *
	 * @return the actual index name used by the index manager
	 */
	String getActualIndexName();
}
