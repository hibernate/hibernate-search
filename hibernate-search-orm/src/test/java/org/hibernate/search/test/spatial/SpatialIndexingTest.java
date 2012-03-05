package org.hibernate.search.test.spatial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Assert;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
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

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, POI.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> pois = fullTextSession.createQuery( "from " + POI.class.getName() ).list();
		for (Object entity : pois) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationOnFieldLevel() throws Exception {
		SimpleDateFormat dateFormat= new SimpleDateFormat("d M yyyy");
		Date date= dateFormat.parse( "10 9 1976" );
		Event event = new Event( 1, "Test", 24.0d, 32.0d, date );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( event );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Event.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Event.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + Event.class.getName() ).list();
		for (Object entity : events) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationOnClassLevel() throws Exception {
		Hotel hotel = new Hotel( 1, "Plazza Athénée", 24.0d, 32.0d, "Luxurious" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( hotel );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Hotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQuery(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Hotel.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + Hotel.class.getName() ).list();
		for (Object entity : events) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSimpleSpatialAnnotationOnClassLevel() throws Exception {
		SimpleHotel hotel = new SimpleHotel( 1, "Plazza Athénée", 24.0d, 32.0d, "Luxurious" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( hotel );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( SimpleHotel.class ).get();

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSimpleSpatialQuery(
				centerLatitude, centerLongitude, 50,
				queryBuilder,
				SimpleHotel.class
		);

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, SimpleHotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSimpleSpatialQuery(
				centerLatitude, centerLongitude, 51,
				queryBuilder,
				SimpleHotel.class
		);

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, SimpleHotel.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + SimpleHotel.class.getName() ).list();
		for (Object entity : events) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				POI.class,
				Event.class,
				Hotel.class,
				SimpleHotel.class
		};
	}
}
