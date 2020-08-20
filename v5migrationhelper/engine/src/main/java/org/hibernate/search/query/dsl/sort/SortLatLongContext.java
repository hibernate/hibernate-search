/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortLatLongContext {

	/**
	 * Sort by the distance to the given longitude
	 * and {@link SortDistanceFieldContext#fromLatitude(double) formerly-defined latitude}.
	 *
	 * @param longitude The reference longitude, part of the coordinates
	 * to which the distance will be computed for each document.
	 */
	SortDistanceFieldAndReferenceContext andLongitude(double longitude);
}
