/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spatial.impl;

import org.hibernate.search.spatial.Coordinates;

/**
 * Normalized latitude,longitude holder (in [-90;90],[-180,180]) with distance and destination computations methods
 *
 * @author Nicolas Helleringer
 * @author Mathieu Perez
 */
public final class Point implements Coordinates {

	private final double latitude;
	private final double longitude;

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @return a point with coordinates given in degrees
	 */
	public static Point fromDegrees(double latitude, double longitude) {
		return new Point( normalizeLatitude( latitude ), normalizeLongitude( longitude ) );
	}

	/**
	 * @param center the coordinates for the Point to be created
	 * @return a Point from given Coordinates. Same instance when given a Point.
	 */
	public static Point fromCoordinates(Coordinates center) {
		if ( center instanceof Point ) {
			return (Point) center;
		}
		else {
			return Point.fromDegrees( center.getLatitude(), center.getLongitude() );
		}
	}

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 * @return a point with coordinates given in degrees
	 */
	public static Point fromDegreesInclusive(double latitude, double longitude) {
		return new Point( normalizeLatitude( latitude ), normalizeLongitudeInclusive( longitude ) );
	}

	/**
	 * @param longitude in degrees
	 * @return longitude normalized in ]-180;+180]
	 */
	public static double normalizeLongitude(double longitude) {
		if ( longitude == ( -GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ) ) {
			return GeometricConstants.LONGITUDE_DEGREE_RANGE / 2;
		}
		else {
			return normalizeLongitudeInclusive( longitude );
		}
	}

	/**
	 * @param longitude in degrees
	 * @return longitude normalized in [-180;+180]
	 */
	public static double normalizeLongitudeInclusive(double longitude) {
		if ( ( longitude < -( GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ) )
				|| ( longitude > ( GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ) ) ) {
			double _longitude;
			// shift 180 and normalize full circle turn
			_longitude = ( ( longitude
					+ ( GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 ) ) % GeometricConstants.WHOLE_CIRCLE_DEGREE_RANGE );
			// as Java % is not a math modulus we may have negative numbers so the unshift is sign dependant
			if ( _longitude < 0 ) {
				_longitude = _longitude + ( GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 );
			}
			else {
				_longitude = _longitude - ( GeometricConstants.LONGITUDE_DEGREE_RANGE / 2 );
			}
			return _longitude;
		}
		else {
			return longitude;
		}
	}

	/**
	 * @param latitude in degrees
	 * @return latitude normalized in [-90;+90]
	 */
	public static double normalizeLatitude(double latitude) {
		if ( latitude > GeometricConstants.LATITUDE_DEGREE_MAX || latitude < GeometricConstants.LATITUDE_DEGREE_MIN ) {
			// shift 90, normalize full circle turn and 'symmetry' on the lat axis with abs
			double _latitude = Math.abs( ( latitude
					+ ( GeometricConstants.LATITUDE_DEGREE_RANGE / 2 ) ) % ( GeometricConstants.WHOLE_CIRCLE_DEGREE_RANGE ) );
			// Push 2nd and 3rd quadran in 1st and 4th by 'symmetry'
			if ( _latitude > GeometricConstants.LATITUDE_DEGREE_RANGE ) {
				_latitude = GeometricConstants.WHOLE_CIRCLE_DEGREE_RANGE - _latitude;
			}
			// unshift
			_latitude = _latitude - ( GeometricConstants.LATITUDE_DEGREE_RANGE / 2 );
			return _latitude;
		}
		else {
			return latitude;
		}
	}

	/**
	 * @param latitude in radians
	 * @param longitude in radians
	 * @return a point with coordinates given in radians
	 */
	public static Point fromRadians(double latitude, double longitude) {
		return fromDegrees( latitude * GeometricConstants.TO_DEGREES_RATIO, longitude * GeometricConstants.TO_DEGREES_RATIO );
	}

	/**
	 * @param latitude in degrees
	 * @param longitude in degrees
	 */
	private Point(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Calculate end of travel point
	 *
	 * @param distance to travel
	 * @param heading of travel in decimal degree
	 * @return arrival point
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Compute destination</a>
	 */
	public Point computeDestination(double distance, double heading) {
		double headingRadian = heading * GeometricConstants.TO_RADIANS_RATIO;

		double destinationLatitudeRadian = Math.asin(
				Math.sin( getLatitudeRad() ) * Math.cos( distance / GeometricConstants.EARTH_MEAN_RADIUS_KM ) + Math.cos(
						getLatitudeRad()
				) * Math.sin( distance / GeometricConstants.EARTH_MEAN_RADIUS_KM ) * Math.cos(
						headingRadian
				)
		);

		double destinationLongitudeRadian = getLongitudeRad() + Math.atan2(
				Math.sin( headingRadian ) * Math.sin(
						distance / GeometricConstants.EARTH_MEAN_RADIUS_KM
				) * Math.cos( getLatitudeRad() ),
				Math.cos( distance / GeometricConstants.EARTH_MEAN_RADIUS_KM ) - Math.sin( getLatitudeRad() ) * Math.sin(
						destinationLatitudeRadian
				)
		);

		return fromRadians( destinationLatitudeRadian, destinationLongitudeRadian );
	}

	/**
	 * Compute distance between two points
	 *
	 * @param other a {@link Point} object.
	 * @return the distance between points
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Distance haversine formula</a>
	 */
	public double getDistanceTo(Point other) {
		return getDistanceTo( other.getLatitude(), other.getLongitude() );
	}

	/**
	 * Compute distance point and other location given by its latitude and longitude in decimal degrees
	 *
	 * @param latitude in decimal degrees
	 * @param longitude in decimal degrees
	 * @return the distance between the points
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Distance haversine formula</a>
	 */
	public double getDistanceTo(final double latitude, final double longitude) {
		double destinationLatitudeRadians = normalizeLatitude( latitude ) * GeometricConstants.TO_RADIANS_RATIO;
		double destinationLongitudeRadians = normalizeLongitude( longitude ) * GeometricConstants.TO_RADIANS_RATIO;
		final double dLat = ( destinationLatitudeRadians - getLatitudeRad() ) / 2.0d;
		final double dLon = ( destinationLongitudeRadians - getLongitudeRad() ) / 2.0d;
		final double a = Math.pow( Math.sin( dLat ), 2 )
				+ Math.pow( Math.sin( dLon ), 2 ) * Math.cos( getLatitudeRad() ) * Math.cos( destinationLatitudeRadians );
		final double c = 2.0d * Math.atan2( Math.sqrt( a ), Math.sqrt( 1.0d - a ) );
		return c * GeometricConstants.EARTH_MEAN_RADIUS_KM;
	}

	@Override
	public Double getLatitude() {
		return latitude;
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	public double getLatitudeRad() {
		return latitude * GeometricConstants.TO_RADIANS_RATIO;
	}

	public double getLongitudeRad() {
		return longitude * GeometricConstants.TO_RADIANS_RATIO;
	}

	@Override
	public int hashCode() {
		return 31 * Double.hashCode( latitude ) + Double.hashCode( longitude );
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj instanceof Point ) {
			Point other = (Point) obj;
			return latitude == other.latitude
					&& longitude == other.longitude;
		}
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Point" );
		sb.append( "{latitude=" ).append( latitude );
		sb.append( ", longitude=" ).append( longitude );
		sb.append( '}' );
		return sb.toString();
	}
}
