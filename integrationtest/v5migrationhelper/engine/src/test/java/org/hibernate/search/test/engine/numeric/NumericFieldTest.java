/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine.numeric;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.dsl.RangeMatchingContext;
import org.hibernate.search.query.dsl.RangeTerminationExcludable;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchITHelper.AssertBuildingHSQueryContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;

public class NumericFieldTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Location.class, Coordinate.class,
			PointOfInterest.class, Position.class, TouristAttraction.class, ScoreBoard.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void setUp() throws Exception {
		prepareData();
	}

	@Test
	public void testIndexAndSearchNumericField() {
		// Range Queries including lower and upper bounds
		assertRangeQuery( "overriddenFieldName", 1, 3 ).as( "Query id " ).hasResultSize( 3 );
		assertRangeQuery( "latitude", -10d, 10d ).as( "Query by double range" ).hasResultSize( 3 );
		assertRangeQuery( "importance", (short) 11, (short) 13 ).as( "Query by short range" ).hasResultSize( 3 );
		assertRangeQuery( "fallbackImportance", Short.valueOf( "11" ), Short.valueOf( "13" ) ).as( "Query by Short range" )
				.hasResultSize( 3 );
		assertRangeQuery( "popularity", (byte) 21, (byte) 23 ).as( "Query by byte range" ).hasResultSize( 3 );
		assertRangeQuery( "fallbackPopularity", Byte.valueOf( "21" ), Byte.valueOf( "23" ) ).as( "Query by Byte range" )
				.hasResultSize( 3 );
		assertRangeQuery( "ranking", 1, 2 ).as( "Query by integer range" ).hasResultSize( 4 );
		assertRangeQuery( "myCounter", 1L, 3L ).as( "Query by long range" ).hasResultSize( 3 );
		assertRangeQuery( "strMultiple", 0.7d, 0.9d ).as( "Query by multi-fields" ).hasResultSize( 2 );

		// Range Queries different bounds
		assertRangeQuery( "overriddenFieldName", 1, 3, true, false ).as( "Query by id excluding upper" ).hasResultSize( 2 );
		assertRangeQuery( "overriddenFieldName", 1, 3, false, false ).as( "Query by id excluding upper and lower" )
				.hasResultSize( 1 );

		// Range Query for embedded entities
		assertRangeQuery( "country.idh", 0.9, 1d ).as( "Range Query for indexed embedded" ).hasResultSize( 2 );

		// Range Query across entities
		assertRangeQuery( "pinPoints.stars", 4, 5 ).as( "Range Query across entities" ).hasResultSize( 1 );

		// Exact Matching Queries
		assertExactQuery( "overriddenFieldName", 1 ).as( "Query id exact" ).matchesExactlyIds( 1 );
		assertExactQuery( "latitude", -10d ).as( "Query double exact" ).matchesExactlyIds( 2 );
		assertExactQuery( "importance", (short) 12 ).as( "Query short exact" ).matchesExactlyIds( 3 );
		assertExactQuery( "popularity", (byte) 22 ).as( "Query byte exact" ).matchesExactlyIds( 3 );
		assertExactQuery( "longitude", -20d ).as( "Query integer exact" ).matchesExactlyIds( 3 );
		assertExactQuery( "myCounter", 4L ).as( "Query long exact" ).matchesExactlyIds( 4 );
		assertExactQuery( "strMultiple", 0.1d ).as( "Query multifield exact" ).matchesExactlyIds( 5 );

		// Delete operation on Numeric Id with overriden field name:
		helper.delete( Location.class, 1, 2, 3, 4, 5 );

		assertRangeQuery( "overriddenFieldName", 1, 6 ).as( "Check for deletion on Query" ).hasResultSize( 0 );
		// and now check also for the real index contents:
		assertRangeQuery( "overriddenFieldName", 1, 6 )
				.projecting( ProjectionConstants.ID )
				.as( "Check for deletion on index projection" )
				.hasResultSize( 0 );
	}

	@TestForIssue(jiraKey = "HSEARCH-1193")
	@Test
	public void testNumericFieldProjections() {
		Query latitudeQuery = helper.queryBuilder( Location.class ).range().onField( "latitude" )
				.from( -20d ).to( -20d ).createQuery();
		helper.assertThatQuery( latitudeQuery ).from( Location.class )
				.projecting( "latitude" )
				.matchesExactlySingleProjections( -20d );

		helper.assertThatQuery( latitudeQuery ).from( Location.class )
				.projecting( "importance", "popularity" )
				.matchesExactlyProjections( new Object[] {
						(short) 10,
						(byte) 20
				} );
	}

	private AssertBuildingHSQueryContext assertExactQuery(String fieldName, Object value) {
		Query matchQuery = helper.queryBuilder( Location.class ).keyword().onField( fieldName )
				.matching( value )
				.createQuery();
		return helper.assertThatQuery( matchQuery ).from( Location.class );
	}

	private AssertBuildingHSQueryContext assertRangeQuery(String fieldName, Object from, Object to) {
		Query query = helper.queryBuilder( Location.class ).range().onField( fieldName )
				.from( from ).to( to )
				.createQuery();
		return helper.assertThatQuery( query ).from( Location.class );
	}

	private AssertBuildingHSQueryContext assertRangeQuery(String fieldName, Object from, Object to, boolean includeLower,
			boolean includeUpper) {
		RangeMatchingContext.FromRangeContext<Object> fromContext = helper.queryBuilder(
				Location.class ).range().onField( fieldName )
				.from( from );
		if ( !includeLower ) {
			fromContext = fromContext.excludeLimit();
		}
		RangeTerminationExcludable toContext = fromContext.to( to );
		if ( !includeUpper ) {
			toContext = toContext.excludeLimit();
		}
		Query query = toContext.createQuery();
		return helper.assertThatQuery( query ).from( Location.class );
	}

	private void prepareData() {
		Location loc1 = new Location( 1, 1L, -20d, -40d, 1, "Random text", 1.5d, countryFor( "England", 0.947 ), (short) 10,
				(byte) 20 );
		loc1.addPinPoints( new PinPoint( 1, 4, loc1 ), new PinPoint( 2, 5, loc1 ) );

		Location loc2 =
				new Location( 2, 2L, -10d, -30d, 1, "Some text", 0.786d, countryFor( "Italy", 0.951 ), (short) 11, (byte) 21 );
		loc2.addPinPoints( new PinPoint( 3, 1, loc2 ), new PinPoint( 4, 2, loc2 ) );

		Location loc3 =
				new Location( 3, 3L, 0d, -20d, 1, "A text", 0.86d, countryFor( "Brazil", 0.813 ), (short) 12, (byte) 22 );
		Location loc4 =
				new Location( 4, 4L, 10d, 0d, 2, "Any text", 0.99d, countryFor( "France", 0.872 ), (short) 13, (byte) 23 );
		Location loc5 =
				new Location( 5, 5L, 20d, 20d, 3, "Random text", 0.1d, countryFor( "India", 0.612 ), (short) 14, (byte) 24 );

		helper.add( loc1, loc2, loc3, loc4, loc5 );

		Coordinate coordinate1 = new Coordinate( (short) 1, -20D, 20D );
		Coordinate coordinate2 = new Coordinate( (short) 2, -30D, 30D );
		helper.add( coordinate1, coordinate2 );

		PointOfInterest poi1 = new PointOfInterest( (byte) 1, -20D, 20D );
		PointOfInterest poi2 = new PointOfInterest( (byte) 2, -30D, 30D );
		helper.add( poi1, poi2 );

		Position position1 = new Position( (byte) 1, -20D, 20D );
		Position position2 = new Position( (byte) 2, -30D, 30D );
		helper.add( position1, position2 );

		TouristAttraction attraction = new TouristAttraction( 1, (short) 23, (short) 46L );
		helper.add( attraction );

		Score score1 = new Score();
		score1.id = 1;
		score1.subscore = 100;

		ScoreBoard scoreboard = new ScoreBoard();
		scoreboard.id = 1L;
		scoreboard.scores.add( score1 );
		helper.add( scoreboard );
	}

	private Country countryFor(String name, double idh) {
		return new Country( name, idh );
	}

	@Indexed
	private static class ScoreBoard {
		@DocumentId
		Long id;

		@IndexedEmbedded(includeEmbeddedObjectId = true, prefix = "score_")
		Set<Score> scores = new HashSet<Score>();

	}

	private static class Score {
		@DocumentId
		@NumericField
		Integer id;

		@Field(name = "beta", store = Store.YES)
		Integer subscore;
	}
}
