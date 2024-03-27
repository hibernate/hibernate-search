/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * The step in a sort definition where the behavior on missing values can be set.
 *
 * @param <N> The type of the next step (returned by {@link DistanceSortMissingValueBehaviorStep#first()}, for example).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface DistanceSortMissingValueBehaviorStep<N> {

	/**
	 * Put documents with missing values last in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The next step.
	 */
	N last();

	/**
	 * Put documents with missing values first in the sorting.
	 *
	 * <p>This instruction is independent of whether the sort is being ascending
	 * or descending.
	 *
	 * @return The next step.
	 */
	N first();

	/**
	 * Give documents with missing values the highest value when sorting.
	 * <p>
	 * This puts documents with missing values last when using ascending order,
	 * or first when using descending order.
	 *
	 * @return The next step.
	 */
	N highest();

	/**
	 * Give documents with missing values the lowest value when sorting.
	 * <p>
	 * This puts documents with missing values first when using ascending order,
	 * or last when using descending order.
	 *
	 * @return The next step.
	 */
	N lowest();

	/**
	 * When documents are missing a value on the sort field, use the given value instead.
	 *
	 * @param value The value to use as a default when a document is missing a value on the sort field.
	 * @return The next step.
	 */
	N use(GeoPoint value);

}
