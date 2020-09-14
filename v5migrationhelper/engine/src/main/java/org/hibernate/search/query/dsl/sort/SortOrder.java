/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortOrder<T> {
	/**
	 * Sort in ascending order.
	 */
	T asc();

	/**
	 * Sort in descending order.
	 */
	T desc();
}
