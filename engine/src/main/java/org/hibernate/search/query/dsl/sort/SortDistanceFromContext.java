/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.spatial.Coordinates;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortDistanceFromContext {

	/**
	 * Sort by the distance to the given {@link Coordinates}.
	 */
	SortDistanceContext fromCoordinates(Coordinates coordinates);

	/**
	 * Sort by the distance to the given latitude
	 * and {@link SortLatLongContext#andLongitude(double) longitude}.
	 */
	SortLatLongContext fromLatitude(double latitude);

}
