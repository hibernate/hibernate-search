package org.hibernate.search.test.spatial;

import java.util.List;

import org.junit.Assert;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spatial.SpatialQueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * Hibernate Search spatial : unit tests on indexing POIs in with Grid and Grid+Distance
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialIndexingTest extends SearchTestCase {

	public void testIndexing() throws Exception {
		POI poi = new POI( 1, "Test", 24.0d, 32.0d, "" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildGridQuery( centerLatitude, centerLongitude, 50, "location" );
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		List results = hibQuery.list();
		Assert.assertEquals( 1, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildGridQuery( centerLatitude, centerLongitude, 1, "location" );
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, POI.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 0, results2.size() );

		org.apache.lucene.search.Query luceneQuery3 = SpatialQueryBuilder.buildDistanceQuery(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, POI.class );
		List results3 = hibQuery3.list();
		Assert.assertEquals( 0, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, POI.class );
		List results4 = hibQuery4.list();
		Assert.assertEquals( 0, results4.size() );

		org.apache.lucene.search.Query luceneQuery5 = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery5 = fullTextSession.createFullTextQuery( luceneQuery5, POI.class );
		List results5 = hibQuery5.list();
		Assert.assertEquals( 1, results5.size() );

		List<?> pois = fullTextSession.createQuery( "from " + POI.class.getName() ).list();
		for (Object entity : pois) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				POI.class
		};
	}
}
