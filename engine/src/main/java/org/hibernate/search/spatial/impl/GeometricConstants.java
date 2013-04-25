/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.spatial.impl;

/**
 * Geometric constants to use in SpatialHelper calculation
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @author Mathieu Perez <mathieu.perez@novacodex.net>
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
	double EARTH_EQUATOR_CIRCUMFERENCE_KM = 40075.017;
	double PROJECTED_LATITUDE_RANGE = Math.PI;
	double PROJECTED_LONGITUDE_RANGE = 2 * Math.PI;
	Point NORTH_POLE = Point.fromDegrees( LATITUDE_DEGREE_MAX, 0 );
	Point SOUTH_POLE = Point.fromDegrees( LATITUDE_DEGREE_MIN, 0 );
}
