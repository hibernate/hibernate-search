/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/**
 * Mode when comparing multiple values.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public enum MultiValuedMode {
	/**
	 * Pick the lowest value.
	 */
	MIN,

	/**
	 * Pick the highest value.
	 */
	MAX,

	/**
	 * Use the sum of all values as sort value.
	 * Only applicable for number based array fields.
	 */
	SUM,

	/**
	 * Use the average of all values as sort value.
	 * Only applicable for number based array fields.
	 */
	AVG,

	/**
	 * Use the median of all values as sort value.
	 * Only applicable for number based array fields.
	 */
	MEDIAN
}
