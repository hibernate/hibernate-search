package org.hibernate.search.test.engine;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.bridge.util.NumericFieldUtils;
import org.hibernate.search.test.SearchTestCase;

import java.io.IOException;
import java.util.List;

public class NumericFieldTest extends SearchTestCase {

	FullTextSession fullTextSession;

	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		prepareData();
	}

	public void tearDown() throws Exception {
		cleanData();
		assertTrue(indexIsEmpty());
		super.tearDown();
	}

	public void testIndexAndSearchNumericField() {
		Transaction tx = fullTextSession.beginTransaction();

		// Range Queries including lower and upper bounds
		assertEquals("Query id ", 3, numericQueryFor("overridenFieldName", 1, 3).size());
		assertEquals("Query by double range", 3, numericQueryFor( "latitude", -10d, 10d ).size() );
		assertEquals("Query by integer range", 4, numericQueryFor( "ranking", 1 ,2 ).size() );
		assertEquals("Query by long range", 3, numericQueryFor( "myCounter", 1L, 3L ).size() );
		assertEquals("Query by multifields", 2, numericQueryFor( "strMultiple", 0.7d, 0.9d).size() );

		// Range Queries different bounds
		assertEquals("Query by id excluding upper", 2, numericQueryFor("overridenFieldName", 1, 3, true, false).size() );
		assertEquals("Query by id excluding upper and lower", 1, numericQueryFor("overridenFieldName", 1, 3, false, false).size() );

		// Range Query for embedded entities
		assertEquals("Range Query for indexed embedded", 2, numericQueryFor("country.idh", 0.9, 1d).size() );

		// Range Query across entities
		assertEquals("Range Query across entities", 1, numericQueryFor("pinPoints.stars", 4, 5).size() );

		// Exact Matching Queries
		assertEquals("Query id exact", 1, doExactQuery("overridenFieldName", 1).getId());
		assertEquals("Query double exact", 2, doExactQuery( "latitude", -10d).getId() );
		assertEquals("Query integer exact", 3, doExactQuery("longitude", -20d).getId() );
		assertEquals("Query long exact", 4, doExactQuery("myCounter",4L).getId() );
		assertEquals("Query multifield exact", 5, doExactQuery("strMultiple",0.1d).getId() );
		
		tx.commit();
		fullTextSession.clear();
		
		// Delete operation on Numeric Id with overriden field name:
		tx = fullTextSession.beginTransaction();
		List allLocations = fullTextSession.createCriteria( Location.class ).list();
		for (Object location : allLocations) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.clear();
		tx = fullTextSession.beginTransaction();
		
		assertEquals("Check for deletion on Query", 0, numericQueryFor("overridenFieldName", 1, 6).size());
		// and now check also for the real index contents:
		Query query = NumericFieldUtils.createNumericRangeQuery("overridenFieldName", 1, 6, true, true);
		FullTextQuery fullTextQuery = fullTextSession
			.createFullTextQuery( query, Location.class )
			.setProjection( ProjectionConstants.DOCUMENT );
		assertEquals("Check for deletion on index projection", 0, fullTextQuery.list().size() );
		
		tx.commit();
	}

	private boolean indexIsEmpty() {
		Directory locationdirectory = fullTextSession.getSearchFactory().getDirectoryProviders(
				Location.class)[0].getDirectory();
		Directory pinPointDirectory = fullTextSession.getSearchFactory().getDirectoryProviders(
				PinPoint.class)[0].getDirectory();
		try {
			int numDocsLocation = IndexReader.open(pinPointDirectory).numDocs();
			int numDocsPinPoint = IndexReader.open(locationdirectory).numDocs();
			return numDocsLocation==0 && numDocsPinPoint==0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PinPoint.class, Location.class };
	}

	private Location doExactQuery(String fieldName, Object value) {
		Query matchQuery = NumericFieldUtils.createExactMatchQuery(fieldName, value);
		return (Location) fullTextSession.createFullTextQuery(matchQuery, Location.class).list().get(0);
	}

	private List numericQueryFor(String fieldName, Object from, Object to) {
		Query query = NumericFieldUtils.createNumericRangeQuery(fieldName, from, to, true, true);
		return fullTextSession.createFullTextQuery(query, Location.class).list();
	}

	private List numericQueryFor(String fieldName, Object from, Object to, boolean includeLower, boolean includeUpper) {
		Query query = NumericFieldUtils.createNumericRangeQuery(fieldName, from, to, includeLower, includeUpper);
		return fullTextSession.createFullTextQuery(query, Location.class).list();
	}

	private void prepareData() {
		Transaction tx = fullTextSession.beginTransaction();
		Location loc1 = new Location(1, 1L, -20d, -40d, 1, "Random text", 1.5d, countryFor("England", 0.947) );
		loc1.addPinPoints(new PinPoint( 1, 4, loc1), new PinPoint( 2, 5, loc1 ) );

		Location loc2 = new Location(2, 2L, -10d, -30d, 1, "Some text", 0.786d, countryFor("Italy", 0.951) );
		loc2.addPinPoints(new PinPoint( 3, 1, loc2), new PinPoint( 4, 2, loc2 ) );

		Location loc3 = new Location(3, 3L, 0d, -20d, 1, "A text", 0.86d, countryFor("Brazil", 0.813) );
		Location loc4 = new Location(4, 4L, 10d, 0d, 2, "Any text", 0.99d, countryFor("France", 0.872) );
		Location loc5 = new Location(5, 5L, 20d, 20d, 3, "Random text", 0.1d, countryFor("India", 0.612) );

		fullTextSession.save(loc1);
		fullTextSession.save(loc2);
		fullTextSession.save(loc3);
		fullTextSession.save(loc4);
		fullTextSession.save(loc5);
		
		tx.commit();
		fullTextSession.clear();
	}
	
	private Country countryFor(String name, double idh) {
		return new Country( name, idh );
	}

	private void cleanData() {
		Transaction tx = fullTextSession.beginTransaction();
		List<Location> locations = fullTextSession.createCriteria(Location.class).list();
		for(Location location: locations) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.close();
	}

}