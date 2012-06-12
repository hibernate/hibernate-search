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
 * Bounding box older for search area on Earth
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @author Mathieu Perez <mathieu.perez@novacodex.net>
 */
public final class Rectangle {

	private final Point lowerLeft;
	private final Point upperRight;

	public Rectangle(Point lowerLeft, Point upperRight) {
		this.lowerLeft = lowerLeft;
		this.upperRight = upperRight;
	}

	/**
	 * Compute appropriate bouding box on Earth with pole and prime meridian crossing checks
	 *
	 * @param center of the search area
	 * @param radius of the search area
	 *
	 * @return a bouding box for the area
	 *
	 * @see <a href="http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates">Bouding box on Earth calculation</a>
	 */
	public static Rectangle fromBoundingCircle(Point center, double radius) {
		// http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
		double minimumLatitude, maximumLatitude;
		double minimumLongitude, maximumLongitude;

		if ( radius > center.getDistanceTo( GeometricConstants.NORTH_POLE ) ) {
			maximumLatitude = GeometricConstants.LATITUDE_DEGREE_MAX;
		}
		else {
			maximumLatitude = center.computeDestination( radius, GeometricConstants.HEADING_NORTH ).getLatitude();
		}

		if ( radius > center.getDistanceTo( GeometricConstants.SOUTH_POLE ) ) {
			minimumLatitude = GeometricConstants.LATITUDE_DEGREE_MIN;
		}
		else {
			minimumLatitude = center.computeDestination( radius, GeometricConstants.HEADING_SOUTH ).getLatitude();
		}

		if ( ( radius > 2 * Math.PI * GeometricConstants.EARTH_MEAN_RADIUS_KM * Math.cos(
				Math.toRadians(
						minimumLatitude
				)
		) ) || ( radius > 2 * Math.PI * GeometricConstants.EARTH_MEAN_RADIUS_KM * Math.cos(
				Math.toRadians(
						maximumLatitude
				)
		) ) ) {
			maximumLongitude = GeometricConstants.LONGITUDE_DEGREE_MAX;
			minimumLongitude = GeometricConstants.LONGITUDE_DEGREE_MIN;
		}
		else {
			Point referencePoint = Point.fromDegrees(
					Math.max(
							Math.abs( minimumLatitude ),
							Math.abs( maximumLatitude )
					), center.getLongitude()
			);
			maximumLongitude = referencePoint.computeDestination( radius, GeometricConstants.HEADING_EAST )
					.getLongitude();
			minimumLongitude = referencePoint.computeDestination( radius, GeometricConstants.HEADING_WEST )
					.getLongitude();
		}

		return new Rectangle(
				Point.fromDegrees( minimumLatitude, minimumLongitude ),
				Point.fromDegrees( maximumLatitude, maximumLongitude )
		);
	}

	public Point getLowerLeft() {
		return lowerLeft;
	}

	public Point getUpperRight() {
		return upperRight;
	}

}