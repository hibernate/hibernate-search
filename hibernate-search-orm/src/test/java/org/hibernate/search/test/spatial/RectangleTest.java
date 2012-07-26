/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
