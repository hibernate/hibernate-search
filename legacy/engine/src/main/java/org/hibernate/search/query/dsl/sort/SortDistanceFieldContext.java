/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.spatial.Coordinates;

/**
 * A context from which one must define the reference coordinates to
 * which the distance will be computed when sorting.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortDistanceFieldContext {

	/**
	 * Sort by the distance to the given {@link Coordinates}.
	 *
	 * @param coordinates The reference coordinates to which the distance
	 * will be computed for each document.
	 */
	SortDistanceFieldAndReferenceContext fromCoordinates(Coordinates coordinates);

	/**
	 * Sort by the distance to the given latitude
	 * and {@link SortLatLongContext#andLongitude(double) subsequently-defined longitude}.
	 *
	 * @param latitude The reference latitude, part of the coordinates
	 * to which the distance will be computed for each document.
	 */
	SortLatLongContext fromLatitude(double latitude);

}
