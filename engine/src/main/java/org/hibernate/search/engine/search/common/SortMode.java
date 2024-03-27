/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.common;


/**
 * Defines how to pick the value to sort on for multi-valued fields.
 */
public enum SortMode {

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
