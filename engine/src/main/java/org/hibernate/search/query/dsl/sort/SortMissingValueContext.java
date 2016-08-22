/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortMissingValueContext<T> {

	/**
	 * Put documents with missing values last in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 */
	T sortLast();

	/**
	 * Put documents with missing values first in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 */
	T sortFirst();

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * <p>Lucene sort API limits this feature to numeric fields. As Hibernate Search sorts are currently
	 * based on the Lucene API underneath, this is only available for numeric fields for all the indexing
	 * services, Elasticsearch included.
	 * <p>Field bridges, if any, will be ignored. Thus the actual numeric value must be provided.
	 *
	 * @throws SearchException If the field is not numeric.
	 */
	T use(Object value);
}
