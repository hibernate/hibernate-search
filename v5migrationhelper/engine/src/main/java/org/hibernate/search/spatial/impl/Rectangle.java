/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.hibernate.search.spatial.Coordinates;

/**
 * Bounding box for search area on Earth
 *
 * @author Nicolas Helleringer
 * @author Mathieu Perez
 */
public final class Rectangle {

	private final Point lowerLeft;
	private final Point upperRight;

	public Rectangle(Point lowerLeft, Point upperRight) {
		this.lowerLeft = lowerLeft;
		this.upperRight = upperRight;
	}

	/**
	 * Compute appropriate bounding box on Earth with pole and prime meridian crossing checks
	 *
	 * @param centerCoordinates of the search area
	 * @param radius of the search area
	 * @return a bounding box for the area
	 * @see <a href="http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates">Bouding box on Earth calculation</a>
	 */
	public static Rectangle fromBoundingCircle(Coordinates centerCoordinates, double radius) {
		Point center = Point.fromCoordinates( centerCoordinates );
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
		) )
				|| ( radius > 2 * Math.PI * GeometricConstants.EARTH_MEAN_RADIUS_KM * Math.cos(
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
				Point.fromDegreesInclusive( minimumLatitude, minimumLongitude ),
				Point.fromDegreesInclusive( maximumLatitude, maximumLongitude )
		);
	}

	public Point getLowerLeft() {
		return lowerLeft;
	}

	public Point getUpperRight() {
		return upperRight;
	}

}
