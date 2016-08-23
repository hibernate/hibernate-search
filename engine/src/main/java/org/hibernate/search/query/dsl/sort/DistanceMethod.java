/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

/**
 * Method to compute the distance between two points.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public enum DistanceMethod {

	/**
	 * Roughly accurate.
	 */
	SLOPPY_ARC,

	/**
	 * Slightly more precise but significantly slower than {@code SLOPPY_ARC}.
	 */
	ARC,

	/**
	 * Faster, but inaccurate on long distances and close to the poles.
	 */
	PLANE
}
