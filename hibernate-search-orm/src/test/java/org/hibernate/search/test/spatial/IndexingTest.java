package org.hibernate.search.test.spatial;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.spatial.Point;
import org.hibernate.search.spatial.SpatialQueryBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class IndexingTest {

	@Test
	public void indexingTest() {
		POI poi = new POI( 1, "Test", 24.0d, 32.0d, "" );

		SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
		Session session = sessionFactory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		try {
			session.save( poi );
			fullTextSession.index( poi );
			fullTextSession.flushToIndexes();
		}
		catch ( Exception e ) {
			e.printStackTrace();
			Assert.fail( "Exception thrown when index point" );
		}

		try {
			Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32

			org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildGridQuery( center, 50, "location" );
			org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
			List results = hibQuery.list();
			Assert.assertEquals( 1, results.size() );

			org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildGridQuery( center, 1, "location" );
			org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, POI.class );
			List results2 = hibQuery2.list();
			Assert.assertEquals( 0, results2.size() );

			org.apache.lucene.search.Query luceneQuery3 = SpatialQueryBuilder.buildDistanceQuery(
					center,
					50,
					"location"
			);
			org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, POI.class );
			List results3 = hibQuery3.list();
			Assert.assertEquals( 0, results3.size() );

			org.apache.lucene.search.Query luceneQuery4 = SpatialQueryBuilder.buildSpatialQuery(
					center,
					50,
					"location"
			);
			org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, POI.class );
			List results4 = hibQuery4.list();
			Assert.assertEquals( 0, results4.size() );

			org.apache.lucene.search.Query luceneQuery5 = SpatialQueryBuilder.buildSpatialQuery(
					center,
					51,
					"location"
			);
			org.hibernate.Query hibQuery5 = fullTextSession.createFullTextQuery( luceneQuery5, POI.class );
			List results5 = hibQuery5.list();
			Assert.assertEquals( 1, results5.size() );

		}
		catch ( Exception e ) {
			e.printStackTrace();
			Assert.fail( "Exception thrown when querying point" );
		}
	}
}
