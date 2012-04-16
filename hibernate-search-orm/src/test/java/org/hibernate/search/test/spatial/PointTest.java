package org.hibernate.search.test.spatial;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.search.spatial.impl.Point;

/**
 * Unit tests for Hibernate Search Spatial point implementation
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class PointTest {
	@Test
	public void normalizeTest() {
		Point point = Point.fromDegrees( 45, 517 );
		Assert.assertEquals( 45, point.getLatitude(), 0 );
		Assert.assertEquals( 157, point.getLongitude(), 0 );

		Point point2 = Point.fromDegrees( 0, -185 );
		Assert.assertEquals( 175, point2.getLongitude(), 0 );
	}

	@Test
	public void computeDestinationTest() {
		Point point = Point.fromDegrees( 45, 4 );

		Point destination = point.computeDestination( 100, 45 );

		Assert.assertEquals( 0.796432523, destination.getLatitudeRad(), 0.00001 );
		Assert.assertEquals( 0.08568597, destination.getLongitudeRad(), 0.00001 );
	}

	@Test
	public void distanceToPoint() {
		Point point = Point.fromDegrees( 45, 4 );
		Point point2 = Point.fromDegrees( 46, 14 );

		double distance = point.getDistanceTo( point2 );

		Assert.assertEquals( 786.7, distance, 0.1 );
	}
}
