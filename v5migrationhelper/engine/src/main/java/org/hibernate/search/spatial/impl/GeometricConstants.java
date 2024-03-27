/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.spatial.impl;

/**
 * Geometric constants to use in SpatialHelper calculation
 *
 * @author Nicolas Helleringer
 * @author Mathieu Perez
 * @see SpatialHelper
 */
public interface GeometricConstants {

	double TO_RADIANS_RATIO = Math.PI / 180.0;
	double TO_DEGREES_RATIO = 180.0 / Math.PI;
	int WHOLE_CIRCLE_DEGREE_RANGE = 360;
	int LONGITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE;
	int LONGITUDE_DEGREE_MIN = -LONGITUDE_DEGREE_RANGE / 2;
	int LONGITUDE_DEGREE_MAX = LONGITUDE_DEGREE_RANGE / 2;
	int LATITUDE_DEGREE_RANGE = WHOLE_CIRCLE_DEGREE_RANGE / 2;
	int LATITUDE_DEGREE_MIN = -LATITUDE_DEGREE_RANGE / 2;
	int LATITUDE_DEGREE_MAX = LATITUDE_DEGREE_RANGE / 2;
	int HEADING_NORTH = 0;
	int HEADING_SOUTH = 180;
	int HEADING_EAST = 90;
	int HEADING_WEST = 270;
	double EARTH_MEAN_RADIUS_KM = 6371.0;
	Point NORTH_POLE = Point.fromDegrees( LATITUDE_DEGREE_MAX, 0 );
	Point SOUTH_POLE = Point.fromDegrees( LATITUDE_DEGREE_MIN, 0 );
}
