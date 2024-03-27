/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.spatial.Coordinates;

/**
 * A context from which one must define the reference coordinates to
 * which the distance will be computed when sorting.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortDistanceFieldContext {

	/**
	 * Sort by the distance to the given {@link Coordinates}.
	 *
	 * @param coordinates The reference coordinates to which the distance
	 * will be computed for each document.
	 * @return {@code this} for method chaining
	 */
	SortDistanceFieldAndReferenceContext fromCoordinates(Coordinates coordinates);

	/**
	 * Sort by the distance to the given latitude
	 * and {@link SortLatLongContext#andLongitude(double) subsequently-defined longitude}.
	 *
	 * @param latitude The reference latitude, part of the coordinates
	 * to which the distance will be computed for each document.
	 * @return {@code this} for method chaining
	 */
	SortLatLongContext fromLatitude(double latitude);

}
