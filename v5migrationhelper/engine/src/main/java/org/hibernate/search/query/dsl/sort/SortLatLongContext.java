/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortLatLongContext {

	/**
	 * Sort by the distance to the given longitude
	 * and {@link SortDistanceFieldContext#fromLatitude(double) formerly-defined latitude}.
	 *
	 * @param longitude The reference longitude, part of the coordinates
	 * to which the distance will be computed for each document.
	 * @return {@code this} for method chaining
	 */
	SortDistanceFieldAndReferenceContext andLongitude(double longitude);
}
