/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

/**
 * Geometric constants to use in SpatialHelper calculation
 *
 * @author Nicolas Helleringer
 * @author Mathieu Perez
 */
public interface GeometricConstants {

	double TO_RADIANS_RATIO = Math.PI / 180.0;
	double TO_DEGREES_RATIO = 180.0 / Math.PI;
	int WHOLE_CIRCLE_DEGREE_RANGE = 360;
	int LONGITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE;
	int LATITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE / 2;
	int LATITUDE_DEGREE_MIN = -LATITUDE_DEGREE_RANGE / 2;
	int LATITUDE_DEGREE_MAX = LATITUDE_DEGREE_RANGE / 2;
	double EARTH_MEAN_RADIUS_KM = 6371.0;
}
