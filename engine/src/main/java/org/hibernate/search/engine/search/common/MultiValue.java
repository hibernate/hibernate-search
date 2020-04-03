/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common;


/**
 * Defines how to pick the value to sort on for multi-valued fields.
 */
public enum MultiValue {

	/**
	 * When a field has multiple values, compute the sum of all the values.
	 */
	SUM,
	/**
	 * When a field has multiple values, pick the lowest value.
	 */
	MIN,
	/**
	 * When a field has multiple values, pick the highest value.
	 */
	MAX,
	/**
	 * When a field has multiple values, compute the average of all the values.
	 */
	AVG,
	/**
	 * When a field has multiple values, compute the median of all the values.
	 */
	MEDIAN

}
