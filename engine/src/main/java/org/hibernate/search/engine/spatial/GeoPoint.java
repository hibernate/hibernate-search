/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.spatial;

/**
 * @author Nicolas Helleringer
 */
public interface GeoPoint {
	/**
	 * @return the latitude, in degrees
	 */
	double getLatitude();

	/**
	 * @return the longitude, in degrees
	 */
	double getLongitude();
}
