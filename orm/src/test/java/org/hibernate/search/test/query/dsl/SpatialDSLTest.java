/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//DO NOT AUTO INDENT THIS FILE.
//MY DSL IS BEAUTIFUL, DUMB INDENTATION IS SCREWING IT UP
public class SpatialDSLTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		Session session = openSession();
		fullTextSession = Search.getFullTextSession( session );
		indexTestData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testSpatialQueries() {
		Transaction transaction = fullTextSession.beginTransaction();
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		Coordinates coordinates = Point.fromDegrees( 24d, 31.5d );
		Query query = builder
				.spatial()
					.onField( "location" )
					.within( 51, Unit.KM )
						.ofCoordinates( coordinates )
					.createQuery();

		List<?> results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test spatial hash based spatial query", 1, results.size() );
		assertEquals( "test spatial hash based spatial query", "Bozo", ( (POI) results.get( 0 ) ).getName() );

		query = builder
				.spatial()
					.onField( "location" )
					.within( 500, Unit.KM )
						.ofLatitude( 48.858333d ).andLongitude( 2.294444d )
					.createQuery();
		results = fullTextSession.createFullTextQuery( query, POI.class ).list();

		assertEquals( "test spatial hash based spatial query", 1, results.size() );
		assertEquals( "test spatial hash based spatial query", "Tour Eiffel", ( (POI) results.get( 0 ) ).getName() );

		transaction.commit();
	}

	private void indexTestData() {
		Transaction tx = fullTextSession.beginTransaction();

		POI poi = new POI( 1, "Tour Eiffel", 48.858333d, 2.294444d, "Monument" );
		fullTextSession.persist( poi );
		poi = new POI( 2, "Bozo", 24d, 32d, "Monument" );
		fullTextSession.persist( poi );

		tx.commit();
		fullTextSession.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				POI.class
		};
	}

}
