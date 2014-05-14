/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import org.hibernate.search.spatial.impl.SpatialHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import org.hibernate.search.spatial.impl.Point;

/**
 * Hibernate Search spatial: helper class to compute spatial hashes indexes, ids, and optimal level for search
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialHashHelperTest {

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
	public void getSpatialHashCellIdTest() {
		Point point = Point.fromDegrees( 45, 4 );

		String cellId = SpatialHelper.getSpatialHashCellId( point, 5 );
		Assert.assertEquals( "0|8", cellId );

		String cellId2 = SpatialHelper.getSpatialHashCellId( point, 7 );
		Assert.assertEquals( "1|32", cellId2 );

		String cellId3 = SpatialHelper.getSpatialHashCellId( point, 14 );
		Assert.assertEquals( "128|4096", cellId3 );

		Point point2 = Point.fromDegrees( -12, -179 );

		String cellId4 = SpatialHelper.getSpatialHashCellId( point2, 5 );
		Assert.assertEquals( "-16|-3", cellId4 );

		String cellId5 = SpatialHelper.getSpatialHashCellId( point2, 7 );
		Assert.assertEquals( "-63|-9", cellId5 );

		String cellId6 = SpatialHelper.getSpatialHashCellId( point2, 14 );
		Assert.assertEquals( "-7969|-1093", cellId6 );
	}

	@Test
	public void findBestSpatialHashLevelForSearchRangeTest() {
		int bestSpatialHashLevel = SpatialHelper.findBestSpatialHashLevelForSearchRange( 50 );

		Assert.assertEquals( 9, bestSpatialHashLevel );

		int bestSpatialHashLevel2 = SpatialHelper.findBestSpatialHashLevelForSearchRange( 1 );

		Assert.assertEquals( 15, bestSpatialHashLevel2 );
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
		Integer spatialHashLevel = SpatialHelper.findBestSpatialHashLevelForSearchRange( radius );

		List<String> cellsIds = SpatialHelper.getSpatialHashCellsIds( center, radius, spatialHashLevel );

		Point edge = null;

		boolean validated = true;

		for ( int heading = 0; heading < 360; heading++ ) {
			edge = center.computeDestination( radius, heading );

			String cellId = SpatialHelper.getSpatialHashCellId( edge, spatialHashLevel );

			validated &= cellsIds.contains( cellId );
		}

		return validated;
	}

}
