/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import static org.junit.Assert.assertEquals;

import org.hibernate.search.spatial.impl.Point;

import org.junit.Test;

/**
 * Unit tests for Hibernate Search Spatial point implementation
 *
 * @author Nicolas Helleringer
 */
public class PointTest {
	@Test
	public void normalizeTest() {
		Point point = Point.fromDegrees( 45, 517 );
		assertEquals( 45, point.getLatitude(), 0 );
		assertEquals( 157, point.getLongitude(), 0 );

		Point point2 = Point.fromDegrees( 0, -185 );
		assertEquals( 175, point2.getLongitude(), 0 );

		Point point3 = Point.fromDegrees( 110, 0 );
		assertEquals( 70, point3.getLatitude(), 0 );

		Point point4 = Point.fromDegrees( -110, 0 );
		assertEquals( -70, point4.getLatitude(), 0 );

		Point point5 = Point.fromDegrees( 185, 0 );
		assertEquals( -5, point5.getLatitude(), 0 );

		Point point6 = Point.fromDegrees( 90, 180 );
		assertEquals( 90, point6.getLatitude(), 0 );
		assertEquals( 180, point6.getLongitude(), 0 );

		Point point7 = Point.fromDegrees( -90, -180 );
		assertEquals( -90, point7.getLatitude(), 0 );
		assertEquals( 180, point7.getLongitude(), 0 );
	}

	@Test
	public void computeDestinationTest() {
		Point point = Point.fromDegrees( 45, 4 );

		Point destination = point.computeDestination( 100, 45 );

		assertEquals( 0.796432523, destination.getLatitudeRad(), 0.00001 );
		assertEquals( 0.08568597, destination.getLongitudeRad(), 0.00001 );
	}

	@Test
	public void distanceToPoint() {
		Point point = Point.fromDegrees( 45, 4 );
		Point point2 = Point.fromDegrees( 46, 14 );

		double distance = point.getDistanceTo( point2 );

		assertEquals( 786.7, distance, 0.1 );
	}
}
