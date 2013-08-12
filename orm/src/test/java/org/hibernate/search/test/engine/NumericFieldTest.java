/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.engine;

import java.math.BigDecimal;
import java.util.List;

import junit.framework.Assert;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.TestForIssue;

public class NumericFieldTest extends SearchTestCase {

	FullTextSession fullTextSession;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		prepareData();
	}

	@Override
	public void tearDown() throws Exception {
		cleanData();
		assertTrue( indexIsEmpty() );
		super.tearDown();
	}

	public void testIndexAndSearchNumericField() {
		Transaction tx = fullTextSession.beginTransaction();

		// Range Queries including lower and upper bounds
		assertEquals( "Query id ", 3, numericQueryFor( "overriddenFieldName", 1, 3 ).size() );
		assertEquals( "Query by double range", 3, numericQueryFor( "latitude", -10d, 10d ).size() );
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
			Assert.assertEquals( Double.valueOf( -20d ), firstProjection[0] );
			List listAgain = fullTextSession.createFullTextQuery( latitudeQuery, Location.class )
					.setProjection( "coordinatePair_x", "coordinatePair_y" )
					.list();
			Assert.assertEquals( 1, listAgain.size() );
			Object[] secondProjection = (Object[]) listAgain.get( 0 );
			Assert.assertEquals( 2, secondProjection.length );
			Assert.assertEquals( Double.valueOf( 1d ), secondProjection[0] );
			Assert.assertEquals( Double.valueOf( 2d ), secondProjection[1] );
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
		return new Class<?>[] { PinPoint.class, Location.class };
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
		Location loc1 = new Location( 1, 1L, -20d, -40d, 1, "Random text", 1.5d, countryFor( "England", 0.947 ), BigDecimal.ONE );
		loc1.addPinPoints( new PinPoint( 1, 4, loc1 ), new PinPoint( 2, 5, loc1 ) );

		Location loc2 = new Location( 2, 2L, -10d, -30d, 1, "Some text", 0.786d, countryFor( "Italy", 0.951 ), BigDecimal.ONE );
		loc2.addPinPoints( new PinPoint( 3, 1, loc2 ), new PinPoint( 4, 2, loc2 ) );

		Location loc3 = new Location( 3, 3L, 0d, -20d, 1, "A text", 0.86d, countryFor( "Brazil", 0.813 ), BigDecimal.TEN );
		Location loc4 = new Location( 4, 4L, 10d, 0d, 2, "Any text", 0.99d, countryFor( "France", 0.872 ), BigDecimal.ONE );
		Location loc5 = new Location( 5, 5L, 20d, 20d, 3, "Random text", 0.1d, countryFor( "India", 0.612 ), BigDecimal.ONE );

		fullTextSession.save( loc1 );
		fullTextSession.save( loc2 );
		fullTextSession.save( loc3 );
		fullTextSession.save( loc4 );
		fullTextSession.save( loc5 );

		tx.commit();
		fullTextSession.clear();
	}

	private Country countryFor(String name, double idh) {
		return new Country( name, idh );
	}

	private void cleanData() {
		Transaction tx = fullTextSession.beginTransaction();
		List<Location> locations = fullTextSession.createCriteria( Location.class ).list();
		for ( Location location : locations ) {
			fullTextSession.delete( location );
		}
		tx.commit();
		fullTextSession.close();
	}

}
