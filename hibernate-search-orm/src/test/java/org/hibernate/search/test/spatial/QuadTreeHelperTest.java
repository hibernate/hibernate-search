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

import org.hibernate.search.spatial.impl.SpatialHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import org.hibernate.search.spatial.impl.Point;

/**
 * Hibernate Search spatial: helper class to compute quad tree indexes, ids, and optimal level for search
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class QuadTreeHelperTest {

	@Test
	public void getCellIndexTest() {
		int cellIndex = SpatialHelper.getCellIndex( 0.1, 0.3, 1 );
		Assert.assertEquals( 0, cellIndex );
		int cellIndex2 = SpatialHelper.getCellIndex( 0.2, 0.3, 1 );
		Assert.assertEquals( 1, cellIndex2 );

		int cellIndex3 = SpatialHelper.getCellIndex( 3, 10, 4 );
		Assert.assertEquals( 4, cellIndex3 );
		int cellIndex4 = SpatialHelper.getCellIndex( 6, 10, 4 );
		Assert.assertEquals( 9, cellIndex4 );
	}

	@Test
	public void getQuadTreeCellIdTest() {
		Point point = Point.fromDegrees( 45, 4 );

		String cellId = SpatialHelper.getQuadTreeCellId( point, 5 );
		Assert.assertEquals( "0|8", cellId );

		String cellId2 = SpatialHelper.getQuadTreeCellId( point, 7 );
		Assert.assertEquals( "1|32", cellId2 );

		String cellId3 = SpatialHelper.getQuadTreeCellId( point, 14 );
		Assert.assertEquals( "128|4096", cellId3 );

		Point point2 = Point.fromDegrees( -12, -179 );

		String cellId4 = SpatialHelper.getQuadTreeCellId( point2, 5 );
		Assert.assertEquals( "-16|-3", cellId4 );

		String cellId5 = SpatialHelper.getQuadTreeCellId( point2, 7 );
		Assert.assertEquals( "-63|-9", cellId5 );

		String cellId6 = SpatialHelper.getQuadTreeCellId( point2, 14 );
		Assert.assertEquals( "-7969|-1093", cellId6 );
	}

	@Test
	public void findBestQuadTreeLevelForSearchRangeTest() {
		int bestQuadTreeLevel = SpatialHelper.findBestQuadTreeLevelForSearchRange( 50 );

		Assert.assertEquals( 9, bestQuadTreeLevel );

		int bestQuadTreeLevel2 = SpatialHelper.findBestQuadTreeLevelForSearchRange( 1 );

		Assert.assertEquals( 15, bestQuadTreeLevel2 );
	}

	@Test
	public void projectedBoundingBoxCellsIdsInclusionTest() {
		Point center = Point.fromDegrees( 45.0d, 32.0d );
		Double radius = 50.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );

		center = Point.fromDegrees( 0.0d, 0.0d );
		radius = 100.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );

		center = Point.fromDegrees( 180.0d, 0.0d );
		radius = 250.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );

		center = Point.fromDegrees( 0.0d, 90.0d );
		radius = 25.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );

		center = Point.fromDegrees( 45.0d, 360.0d );
		radius = 100.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );

		center = Point.fromDegrees( -147.0d, -24.0d );
		radius = 73.0d;
		Assert.assertTrue( projectedBoundingBoxCellsIdsInclusionTest( center, radius ) );
	}

	public boolean projectedBoundingBoxCellsIdsInclusionTest( Point center, Double radius) {
		Integer quadTreeLevel = SpatialHelper.findBestQuadTreeLevelForSearchRange( radius );

		List<String> cellsIds = SpatialHelper.getQuadTreeCellsIds( center, radius, quadTreeLevel );

		Point edge = null;

		boolean validated = true;

		for ( int heading = 0; heading < 360; heading++ ) {
			edge = center.computeDestination( radius, heading );

			String cellId = SpatialHelper.getQuadTreeCellId( edge, quadTreeLevel );

			validated &= cellsIds.contains( cellId );
		}

		return validated;
	}

}
