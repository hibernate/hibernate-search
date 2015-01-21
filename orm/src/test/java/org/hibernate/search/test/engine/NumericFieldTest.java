/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.math.BigDecimal;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NumericFieldTest extends SearchTestBase {

	FullTextSession fullTextSession;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
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
		Transaction tx = fullTextSession.beginTransaction();

		// Range Queries including lower and upper bounds
		assertEquals( "Query id ", 3, numericQueryFor( "overriddenFieldName", 1, 3 ).size() );
		assertEquals( "Query by double range", 3, numericQueryFor( "latitude", -10d, 10d ).size() );
		assertEquals( "Query by short range", 3, numericQueryFor( "importance", (short) 11, (short) 13 ).size() );
		assertEquals( "Query by Short range", 3, numericQueryFor( "fallbackImportance", Short.valueOf( "11" ), Short.valueOf( "13" ) ).size() );
		assertEquals( "Query by byte range", 3, numericQueryFor( "popularity", (byte) 21, (byte) 23 ).size() );
		assertEquals( "Query by Byte range", 3, numericQueryFor( "fallbackPopularity", Byte.valueOf( "21" ), Byte.valueOf( "23" ) ).size() );
		assertEquals( "Query by integer range", 4, numericQueryFor( "ranking", 1, 2 ).size() );
		assertEquals( "Query by long range", 3, numericQueryFor( "myCounter", 1L, 3L ).size() );
		assertEquals( "Query by multi-fields", 2, numericQueryFor( "strMultiple", 0.7d, 0.9d ).size() );
		assertEquals( "Query on custom bridge by range", 4, numericQueryFor( "visibleStars", -100L, 500L ).size() );

		// Range Queries different bounds
		assertEquals( "Query by id excluding upper", 2, numericQueryFor( "overriddenFieldName", 1, 3, true, false ).size() );
		assertEquals( "Query by id excluding upper and lower", 1, numericQueryFor( "overriddenFieldName", 1, 3, false, false ).size() );

		// Range Query for embedded entities
		assertEquals( "Range Query for indexed embedded", 2, numericQueryFor( "country.idh", 0.9, 1d ).size() );

		// Range Query across entities
		assertEquals( "Range Query across entities", 1, numericQueryFor( "pinPoints.stars", 4, 5 ).size() );

		// Exact Matching Queries
		assertEquals( "Query id exact", 1, doExactQuery( "overriddenFieldName", 1 ).getId() );
		assertEquals( "Query double exact", 2, doExactQuery( "latitude", -10d ).getId() );
		assertEquals( "Query short exact", 3, doExactQuery( "importance", 12 ).getId() );
		assertEquals( "Query byte exact", 3, doExactQuery( "popularity", 22 ).getId() );
		assertEquals( "Query integer exact", 3, doExactQuery( "longitude", -20d ).getId() );
		assertEquals( "Query long exact", 4, doExactQuery( "myCounter", 4L ).getId() );
		assertEquals( "Query multifield exact", 5, doExactQuery( "strMultiple", 0.1d ).getId() );
		assertEquals( "Query on custom bridge exact", 3, doExactQuery( "visibleStars", 1000L ).getId() );

		tx.commit();
		fullTextSession.clear();

		// Delete operation on Numeric Id with overriden field name:
		tx = fullTextSession.beginTransaction();
		List allLocations = fullTextSession.createCriteria( Location.class ).list();
		for ( Object location : allLocations ) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();

		assertEquals( "Check for deletion on Query", 0, numericQueryFor( "overriddenFieldName", 1, 6 ).size() );
		// and now check also for the real index contents:
		Query query = NumericFieldUtils.createNumericRangeQuery( "overriddenFieldName", 1, 6, true, true );
		FullTextQuery fullTextQuery = fullTextSession
				.createFullTextQuery( query, Location.class )
				.setProjection( ProjectionConstants.DOCUMENT );
		assertEquals( "Check for deletion on index projection", 0, fullTextQuery.list().size() );

		tx.commit();
	}

	@TestForIssue(jiraKey = "HSEARCH-1193")
	@Test
	public void testNumericFieldProjections() {
		Transaction tx = fullTextSession.beginTransaction();
		try {
			Query latitudeQuery = NumericFieldUtils.createNumericRangeQuery( "latitude", -20d, -20d, true, true );
			List list = fullTextSession.createFullTextQuery( latitudeQuery, Location.class )
					.setProjection( "latitude" )
					.list();
			Assert.assertEquals( 1, list.size() );
			Object[] firstProjection = (Object[]) list.get( 0 );
			Assert.assertEquals( 1, firstProjection.length );
			Assert.assertEquals( -20d, firstProjection[0] );
			List listAgain = fullTextSession.createFullTextQuery( latitudeQuery, Location.class )
					.setProjection( "coordinatePair_x", "coordinatePair_y", "importance", "popularity" )
					.list();
			Assert.assertEquals( 1, listAgain.size() );
			Object[] secondProjection = (Object[]) listAgain.get( 0 );
			Assert.assertEquals( 4, secondProjection.length );
			Assert.assertEquals( 1d, secondProjection[0] );
			Assert.assertEquals( 2d, secondProjection[1] );
			Assert.assertEquals( (short) 10, secondProjection[2] );
			Assert.assertEquals( (byte) 20, secondProjection[3] );
		}
		finally {
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testShortDocumentIdExplicitlyMappedAsNumericField() {
		Transaction tx = fullTextSession.beginTransaction();
		try {
			Query query = NumericFieldUtils.createNumericRangeQuery( "myId", (short) 1, (short) 1, true, true );
			@SuppressWarnings("unchecked")
			List<Coordinate> list = fullTextSession.createFullTextQuery( query, Coordinate.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (short) 1, list.iterator().next().getId() );
		}
		finally {
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testByteDocumentIdExplicitlyMappedAsNumericField() {
		Transaction tx = fullTextSession.beginTransaction();
		try {
			Query query = NumericFieldUtils.createNumericRangeQuery( "myId", (byte) 1, (byte) 1, true, true );
			@SuppressWarnings("unchecked")
			List<PointOfInterest> list = fullTextSession.createFullTextQuery( query, PointOfInterest.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (byte) 1, list.iterator().next().getId() );
		}
		finally {
			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-997")
	public void testByteDocumentIdMappedAsStringFieldByDefault() {
		Transaction tx = fullTextSession.beginTransaction();
		try {
			Query query = TermRangeQuery.newStringRange( "id", "1", "1", true, true );
			@SuppressWarnings("unchecked")
			List<Position> list = fullTextSession.createFullTextQuery( query, Position.class )
					.list();
			Assert.assertEquals( 1, list.size() );
			Assert.assertEquals( (byte) 1, list.iterator().next().getId() );
		}
		finally {
			tx.commit();
		}
	}

	private boolean indexIsEmpty() {
		int numDocsLocation = countSizeForType( Location.class );
		int numDocsPinPoint = countSizeForType( PinPoint.class );
		return numDocsLocation == 0 && numDocsPinPoint == 0;
	}

	private int countSizeForType(Class<?> type) {
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PinPoint.class, Location.class, Coordinate.class, PointOfInterest.class, Position.class };
	}

	private Location doExactQuery(String fieldName, Object value) {
		Query matchQuery = NumericFieldUtils.createExactMatchQuery( fieldName, value );
		return (Location) fullTextSession.createFullTextQuery( matchQuery, Location.class ).list().get( 0 );
	}

	private List numericQueryFor(String fieldName, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, true, true );
		return fullTextSession.createFullTextQuery( query, Location.class ).list();
	}

	private List numericQueryFor(String fieldName, Object from, Object to, boolean includeLower, boolean includeUpper) {
		Query query = NumericFieldUtils.createNumericRangeQuery( fieldName, from, to, includeLower, includeUpper );
		return fullTextSession.createFullTextQuery( query, Location.class ).list();
	}

	private void prepareData() {
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

		tx.commit();
		fullTextSession.clear();
	}

	private Country countryFor(String name, double idh) {
		return new Country( name, idh );
	}

	@SuppressWarnings("unchecked")
	private void cleanData() {
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

		tx.commit();
		fullTextSession.close();
	}

}
