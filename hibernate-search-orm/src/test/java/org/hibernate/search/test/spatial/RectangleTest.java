package org.hibernate.search.test.spatial;

import org.junit.Assert;
import org.junit.Test;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.impl.Rectangle;

/**
 * Unit tests for Hibernate Search spatial rectangle implementation
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class RectangleTest {
	@Test
	public void boundingBoxTest() {
		Point center = Point.fromDegrees( 45, 4 );
		Rectangle rectangle = Rectangle.fromBoundingCircle( center, 50 );

		Assert.assertEquals( 44.550339, rectangle.getLowerLeft().getLatitude(), 0.000001d );
		Assert.assertEquals( 3.359047, rectangle.getLowerLeft().getLongitude(), 0.000001d );
		Assert.assertEquals( 45.449660, rectangle.getUpperRight().getLatitude(), 0.000001d );
		Assert.assertEquals( 4.640952, rectangle.getUpperRight().getLongitude(), 0.000001d );
	}
}
