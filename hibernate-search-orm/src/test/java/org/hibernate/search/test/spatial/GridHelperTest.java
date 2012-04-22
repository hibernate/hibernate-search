package org.hibernate.search.test.spatial;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import org.hibernate.search.spatial.impl.GridHelper;
import org.hibernate.search.spatial.impl.Point;
/**
 * Hibernate Search spatial : helper class to compute grid indexes, ids, and optimal level for search
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class GridHelperTest {

	@Test
	public void getCellIndexTest() {
		int cellIndex = GridHelper.getCellIndex( 0.1, 0.3, 1 );
		Assert.assertEquals( 0, cellIndex );
		int cellIndex2 = GridHelper.getCellIndex( 0.2, 0.3, 1 );
		Assert.assertEquals( 1, cellIndex2 );

		int cellIndex3 = GridHelper.getCellIndex( 3, 10, 4 );
		Assert.assertEquals( 4, cellIndex3 );
		int cellIndex4 = GridHelper.getCellIndex( 6, 10, 4 );
		Assert.assertEquals( 9, cellIndex4 );
	}

	@Test
	public void getGridCellIdTest() {
		Point point = Point.fromDegrees( 45, 4 );

		String cellId = GridHelper.getGridCellId( point, 5 );
		Assert.assertEquals( "0|8", cellId );

		String cellId2 = GridHelper.getGridCellId( point, 7 );
		Assert.assertEquals( "1|32", cellId2 );

		String cellId3 = GridHelper.getGridCellId( point, 14 );
		Assert.assertEquals( "128|4096", cellId3 );

		Point point2 = Point.fromDegrees( -12, -179 );

		String cellId4 = GridHelper.getGridCellId( point2, 5 );
		Assert.assertEquals( "-16|-3", cellId4 );

		String cellId5 = GridHelper.getGridCellId( point2, 7 );
		Assert.assertEquals( "-63|-9", cellId5 );

		String cellId6 = GridHelper.getGridCellId( point2, 14 );
		Assert.assertEquals( "-7969|-1093", cellId6 );
	}

	@Test
	public void getGridCellIdsTest() {
		Point point = Point.fromDegrees( 45, 4 );

		Map<Integer, String> cellsIdsByGridLevel = GridHelper.getGridCellsIds( point, 0, 15 );

		Assert.assertEquals( "0|8", cellsIdsByGridLevel.get( 5 ) );
		Assert.assertEquals( "1|32", cellsIdsByGridLevel.get( 7 ) );
		Assert.assertEquals( "128|4096", cellsIdsByGridLevel.get( 14 ) );
	}

	@Test
	public void findBestGridLevelForSearchRangeTest() {
		int bestGridLevel = GridHelper.findBestGridLevelForSearchRange( 50 );

		Assert.assertEquals( 9, bestGridLevel );

		int bestGridLevel2 = GridHelper.findBestGridLevelForSearchRange( 1 );

		Assert.assertEquals( 15, bestGridLevel2 );
	}
}
