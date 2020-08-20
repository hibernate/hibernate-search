/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial;

/**
 * Minimum interface for a field/method to be spatial hash indexable
 *
 * @author Nicolas Helleringer
 */
public interface Coordinates {
	/**
	 * @return the latitude in degrees
	 */
	Double getLatitude();

	/**
	 * @return the longitude in degrees
	 */
	Double getLongitude();
}
