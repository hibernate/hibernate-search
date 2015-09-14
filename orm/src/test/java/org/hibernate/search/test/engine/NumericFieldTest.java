/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.FieldSettingsDescriptor.Type;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NumericFieldTest extends SearchTestBase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		prepareData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		cleanData();
		assertTrue( indexIsEmpty() );
		super.tearDown();
	}

	@Test
	public void testIndexAndSearchNumericField() {
		try ( Session session = openSession() ) {
			FullTextSession fts = Search.getFullTextSession( session );
			Transaction tx = fts.beginTransaction();

			// Range Queries including lower and upper bounds
			assertEquals( "Query id ", 3, numericQueryFor( fts, "overriddenFieldName", 1, 3 ).size() );
			assertEquals( "Query by double range", 3, numericQueryFor( fts, "latitude", -10d, 10d ).size() );
			assertEquals( "Query by short range", 3, numericQueryFor( fts, "importance", (short) 11, (short) 13 ).size() );
			assertEquals( "Query by Short range", 3, numericQueryFor( fts, "fallbackImportance", Short.valueOf( "11" ), Short.valueOf( "13" ) ).size() );
			assertEquals( "Query by byte range", 3, numericQueryFor( fts, "popularity", (byte) 21, (byte) 23 ).size() );
			assertEquals( "Query by Byte range", 3, numericQueryFor( fts, "fallbackPopularity", Byte.valueOf( "21" ), Byte.valueOf( "23" ) ).size() );
			assertEquals( "Query by integer range", 4, numericQueryFor( fts, "ranking", 1, 2 ).size() );
			assertEquals( "Query by long range", 3, numericQueryFor( fts, "myCounter", 1L, 3L ).size() );
			assertEquals( "Query by multi-fields", 2, numericQueryFor( fts, "strMultiple", 0.7d, 0.9d ).size() );
			assertEquals( "Query on custom bridge by range", 4, numericQueryFor( fts, "visibleStars", -100L, 500L ).size() );

			// Range Queries different bounds
			assertEquals( "Query by id excluding upper", 2, numericQueryFor( fts, "overriddenFieldName", 1, 3, true, false ).size() );
			assertEquals( "Query by id excluding upper and lower", 1, numericQueryFor( fts, "overriddenFieldName", 1, 3, false, false ).size() );

			// Range Query for embedded entities
			assertEquals( "Range Query for indexed embedded", 2, numericQueryFor( fts, "country.idh", 0.9, 1d ).size() );

			// Range Query across entities
			assertEquals( "Range Query across entities", 1, numericQueryFor( fts, "pinPoints.stars", 4, 5 ).size() );

			// Exact Matching Queries
			assertEquals( "Query id exact", 1, doExactQuery( fts, "overriddenFieldName", 1 ).getId() );
			assertEquals( "Query double exact", 2, doExactQuery( fts, "latitude", -10d ).getId() );
			assertEquals( "Query short exact", 3, doExactQuery( fts, "importance", 12 ).getId() );
			assertEquals( "Query byte exact", 3, doExactQuery( fts, "popularity", 22 ).getId() );
			assertEquals( "Query integer exact", 3, doExactQuery( fts, "longitude", -20d ).getId() );
			assertEquals( "Query long exact", 4, doExactQuery( fts, "myCounter", 4L ).getId() );
			assertEquals( "Query multifield exact", 5, doExactQuery( fts, "strMultiple", 0.1d ).getId() );
			assertEquals( "Query on custom bridge exact", 3, doExactQuery( fts, "visibleStars", 1000L ).getId() );

			tx.commit();
			fts.clear();

			// Delete operation on Numeric Id with overriden field name:
			tx = fts.beginTransaction();
			List<?> allLocations = fts.createCriteria( Location.class ).list();
			for ( Object location : allLocations ) {
				fts.delete( location );
			}
			tx.commit();
			fts.clear();
			tx = fts.beginTransaction();

			assertEquals( "Check for deletion on Query", 0, numericQueryFor( fts, "overriddenFieldName", 1, 6 ).size() );
			// and now check also for the real index contents:
			Query query = NumericFieldUtils.createNumericRangeQuery( "overriddenFieldName", 1, 6, true, true );
			FullTextQuery fullTextQuery = fts
					.createFullTextQuery( query, Location.class )
					.setProjection( ProjectionConstants.DOCUMENT );
			assertEquals( "Check for deletion on index projection", 0, fullTextQuery.list().size() );

			tx.commit();
		}
	}

	@TestForIssue(jiraKey = "HSEARCH-1193")
	@Test
	public void testNumericFieldProjections() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			Query latitudeQuery = NumericFieldUtils.createNumericRangeQuery( "latitude", -20d, -20d, true, true );
			List<?> list = fullTextSession.createFullTextQuery( latitudeQuery, Location.class )
					.setProjection( "latitude" )
					.list();
			Assert.assertEquals( 1, list.size() );
			Object[] firstProjection = (Object[]) list.get( 0 );
			Assert.assertEquals( 1, firstProjection.length );
			Assert.assertEquals( -20d, firstProjection[0] );
			List<?> listAgain = fullTextSession.createFullTextQuery( latitudeQuery, Location.class )
					.setProjection( "coordinatePair_x", "coordinatePair_y", "importance", "popularity" )
					.list();
			Assert.assertEquals( 1, listAgain.size() );
			Object[] secondProjection = (Object[]) listAgain.get( 0 );
			Assert.assertEquals( 4, secondProjection.length );
			Assert.assertEquals( 1d, secondProjection[0] );
			Assert.assertEquals( 2d, secondProjection[1] );
			Assert.assertEquals( (short) 10, secondProjection[2] );
			Assert.assertEquals( (byte) 20, secondProjection[3] );
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testShortDocumentIdExplicitlyMappedAsNumericField() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			Query query = NumericFieldUtils.createNumericRangeQuery( "myId", (short) 1, (short) 1, true, true );
			@SuppressWarnings("unchecked")
			List<Coordinate> list = fullTextSession.createFullTextQuery( query, Coordinate.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (short) 1, list.iterator().next().getId() );
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testByteDocumentIdExplicitlyMappedAsNumericField() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			Query query = NumericFieldUtils.createNumericRangeQuery( "myId", (byte) 1, (byte) 1, true, true );
			@SuppressWarnings("unchecked")
			List<PointOfInterest> list = fullTextSession.createFullTextQuery( query, PointOfInterest.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (byte) 1, list.iterator().next().getId() );
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testByteDocumentIdMappedAsStringFieldByDefault() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			Query query = TermRangeQuery.newStringRange( "id", "1", "1", true, true );
			@SuppressWarnings("unchecked")
			List<Position> list = fullTextSession.createFullTextQuery( query, Position.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (byte) 1, list.iterator().next().getId() );
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	public void testOneOfSeveralFieldsIsNumeric() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			QueryContextBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder();
			Query query = queryBuilder.forEntity( TouristAttraction.class ).get().all().createQuery();

			@SuppressWarnings("unchecked")
			List<Object[]> list = fullTextSession.createFullTextQuery( query, TouristAttraction.class )
					.setProjection( ProjectionConstants.DOCUMENT )
					.list();

			assertEquals( 1, list.size() );
			Document document = (Document) list.iterator().next()[0];

			IndexableField scoreNumeric = document.getField( "scoreNumeric" );
			assertThat( scoreNumeric.numericValue() ).isEqualTo( 23 );

			IndexableField scoreString = document.getField( "scoreString" );
			assertThat( scoreString.numericValue() ).isNull();
			assertThat( scoreString.stringValue() ).isEqualTo( "23" );

			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	public void testSomeOfSeveralFieldsAreNumeric() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );

			Set<FieldDescriptor> fields = fullTextSession.getSearchFactory()
					.getIndexedTypeDescriptor( TouristAttraction.class )
					.getProperty( "rating" )
					.getIndexedFields();

			assertThat( fields ).onProperty( "name" )
					.containsOnly( "rating", "ratingNumericPrecision1", "ratingNumericPrecision2");

			for ( FieldDescriptor field : fields ) {
				if ( "ratingNumericPrecision1".equals( field.getName() ) ) {
					assertThat( field.getType() ).isEqualTo( Type.NUMERIC );
					assertThat( field.as( NumericFieldSettingsDescriptor.class ).precisionStep() ).isEqualTo( 1 );
				}
				else if ( "ratingNumericPrecision2".equals( field.getName() ) ) {
					assertThat( field.getType() ).isEqualTo( Type.NUMERIC );
					assertThat( field.as( NumericFieldSettingsDescriptor.class ).precisionStep() ).isEqualTo( 2 );
				}
				else {
					assertThat( field.getType() ).isEqualTo( Type.BASIC );
				}
			}
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	public void testNumericMappingOfEmbeddedFields() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			QueryContextBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder();
			Query query = queryBuilder.forEntity( ScoreBoard.class ).get().all().createQuery();

			@SuppressWarnings("unchecked")
			List<Object[]> list = fullTextSession.createFullTextQuery( query, ScoreBoard.class )
					.setProjection( ProjectionConstants.DOCUMENT )
					.list();

			assertEquals( 1, list.size() );
			Document document = (Document) list.iterator().next()[0];

			IndexableField scoreNumeric = document.getField( "score_id" );
			assertThat( scoreNumeric.numericValue() ).isEqualTo( 1 );

			IndexableField beta = document.getField( "score_beta" );
			assertThat( beta.numericValue() ).isEqualTo( 100 );

			tx.commit();
		}
	}

	private boolean indexIsEmpty() {
		int numDocsLocation = countSizeForType( Location.class );
		int numDocsPinPoint = countSizeForType( PinPoint.class );
		return numDocsLocation == 0 && numDocsPinPoint == 0;
	}

	private int countSizeForType(Class<?> type) {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			SearchFactory searchFactory = fullTextSession.getSearchFactory();
			int numDocs = -1; // to have it fail in case of errors
			IndexReader locationIndexReader = searchFactory.getIndexReaderAccessor().open( type );
			try {
				numDocs = locationIndexReader.numDocs();
			}
			finally {
				searchFactory.getIndexReaderAccessor().close( locationIndexReader );
			}
			return numDocs;
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ PinPoint.class, Location.class, Coordinate.class,
			PointOfInterest.class, Position.class, TouristAttraction.class, ScoreBoard.class, Score.class };
	}

	private Location doExactQuery(FullTextSession fullTextSession, String fieldName, Object value) {
		Query matchQuery = NumericFieldUtils.createExactMatchQuery( fieldName, value );
		return (Location) fullTextSession.createFullTextQuery( matchQuery, Location.class ).list().get( 0 );
	}

	private List<?> numericQueryFor(FullTextSession fullTextSession, String fieldName, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, true, true );
		return fullTextSession.createFullTextQuery( query, Location.class ).list();
	}

	private List<?> numericQueryFor(FullTextSession fullTextSession, String fieldName, Object from, Object to, boolean includeLower, boolean includeUpper) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, includeLower, includeUpper );
		return fullTextSession.createFullTextQuery( query, Location.class ).list();
	}

	private void prepareData() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			Location loc1 = new Location( 1, 1L, -20d, -40d, 1, "Random text", 1.5d, countryFor( "England", 0.947 ), BigDecimal.ONE, (short) 10, (byte) 20 );
			loc1.addPinPoints( new PinPoint( 1, 4, loc1 ), new PinPoint( 2, 5, loc1 ) );

			Location loc2 = new Location( 2, 2L, -10d, -30d, 1, "Some text", 0.786d, countryFor( "Italy", 0.951 ), BigDecimal.ONE, (short) 11, (byte) 21 );
			loc2.addPinPoints( new PinPoint( 3, 1, loc2 ), new PinPoint( 4, 2, loc2 ) );

			Location loc3 = new Location( 3, 3L, 0d, -20d, 1, "A text", 0.86d, countryFor( "Brazil", 0.813 ), BigDecimal.TEN, (short) 12, (byte) 22 );
			Location loc4 = new Location( 4, 4L, 10d, 0d, 2, "Any text", 0.99d, countryFor( "France", 0.872 ), BigDecimal.ONE, (short) 13, (byte) 23 );
			Location loc5 = new Location( 5, 5L, 20d, 20d, 3, "Random text", 0.1d, countryFor( "India", 0.612 ), BigDecimal.ONE, (short) 14, (byte) 24 );

			fullTextSession.save( loc1 );
			fullTextSession.save( loc2 );
			fullTextSession.save( loc3 );
			fullTextSession.save( loc4 );
			fullTextSession.save( loc5 );

			Coordinate coordinate1 = new Coordinate( (short) 1, -20D, 20D );
			Coordinate coordinate2 = new Coordinate( (short) 2, -30D, 30D );
			fullTextSession.save( coordinate1 );
			fullTextSession.save( coordinate2 );

			PointOfInterest poi1 = new PointOfInterest( (byte) 1, -20D, 20D );
			PointOfInterest poi2 = new PointOfInterest( (byte) 2, -30D, 30D );
			fullTextSession.save( poi1 );
			fullTextSession.save( poi2 );

			Position position1 = new Position( (byte) 1, -20D, 20D );
			Position position2 = new Position( (byte) 2, -30D, 30D );
			fullTextSession.save( position1 );
			fullTextSession.save( position2 );

			TouristAttraction attraction = new TouristAttraction( 1, (short) 23, (short) 46L );
			fullTextSession.save( attraction );

			Score score1 = new Score();
			score1.id = 1;
			score1.subscore = 100;

			fullTextSession.save( score1 );

			ScoreBoard scoreboard = new ScoreBoard();
			scoreboard.id = 1l;
			scoreboard.scores.add( score1 );

			fullTextSession.save( scoreboard );

			tx.commit();
		}
	}

	private Country countryFor(String name, double idh) {
		return new Country( name, idh );
	}

	@SuppressWarnings("unchecked")
	private void cleanData() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();
			List<Location> locations = fullTextSession.createCriteria( Location.class ).list();
			for ( Location location : locations ) {
				fullTextSession.delete( location );
			}

			List<Coordinate> coordinates = fullTextSession.createCriteria( Coordinate.class ).list();
			for ( Coordinate coordinate : coordinates ) {
				fullTextSession.delete( coordinate );
			}

			List<PointOfInterest> pois = fullTextSession.createCriteria( PointOfInterest.class ).list();
			for ( PointOfInterest poi : pois ) {
				fullTextSession.delete( poi );
			}

			List<Position> positions = fullTextSession.createCriteria( Position.class ).list();
			for ( Position position : positions ) {
				fullTextSession.delete( position );
			}

			List<ScoreBoard> scoreboards = fullTextSession.createCriteria( ScoreBoard.class ).list();
			for ( ScoreBoard scoreboard : scoreboards ) {
				fullTextSession.delete( scoreboard );
			}

			List<Score> scores = fullTextSession.createCriteria( Score.class ).list();
			for ( Score score : scores ) {
				fullTextSession.delete( score );
			}

			tx.commit();
		}
	}

	@Indexed @Entity
	static class ScoreBoard {

		@Id
		Long id;

		@IndexedEmbedded(includeEmbeddedObjectId = true, prefix = "score_")
		@OneToMany
		Set<Score> scores = new HashSet<Score>();

	}

	@Indexed @Entity
	static class Score {

		@Id
		@NumericField
		Integer id;

		@Field(name = "beta", store = Store.YES)
		Integer subscore;
	}
}
