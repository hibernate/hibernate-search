package org.hibernate.search.test.spatial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
		POI poi2= new POI(  2, "Test2", 0.0d, -179.0d, "" );
		POI poi3= new POI(  3, "Test3", 0.0d, 179.0d, "" );
		POI poi4= new POI(  4, "Test4", 89.0d, 1.0d, "" );
		POI poi5= new POI(  5, "Test5", -90.0d, 17.0d, "" );
		POI poi6= new POI(  6, "Test6", 47.0d, 154.0d, "" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi );
		fullTextSession.save( poi2 );
		fullTextSession.save( poi3 );
		fullTextSession.save( poi4 );
		fullTextSession.save( poi5 );
		fullTextSession.save( poi6 );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQueryByGrid(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByGrid(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, POI.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		org.apache.lucene.search.Query luceneQuery3 = SpatialQueryBuilder.buildSpatialQueryByGrid(
				0.0d,
				180.0d,
				112,
				"location"
		);
		org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, POI.class );
		List results3 = hibQuery3.list();
		Assert.assertEquals( 2, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = SpatialQueryBuilder.buildSpatialQueryByGrid(
				0.0d,
				0.0d,
				100000,
				"location"
		);
		org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, POI.class );
		List results4 = hibQuery4.list();
		Assert.assertEquals( 6, results4.size() );

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

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQueryByGrid(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Event.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByGrid(
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

	public void testSpatialAnnotationOnFieldLevelRangeMode() throws Exception {
		SimpleDateFormat dateFormat= new SimpleDateFormat("d M yyyy");
		Date date= dateFormat.parse( "10 9 1976" );
		RangeEvent event = new RangeEvent( 1, "Test", 24.0d, 32.0d, date );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( event );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQueryByRange(
				centerLatitude,
				centerLongitude,
				50,
				"location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeEvent.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByRange(
				centerLatitude,
				centerLongitude,
				51,
				"location"
		);
		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeEvent.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + RangeEvent.class.getName() ).list();
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

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQueryByGrid(
				centerLatitude,
				centerLongitude,
				50,
				"hotel_location"
		);
		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Hotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByGrid(
				centerLatitude,
				centerLongitude,
				51,
				"hotel_location"
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

	public void testSpatialAnnotationOnClassLevelRangeMode() throws Exception {
		RangeHotel hotel = new RangeHotel( 1, "Plazza Athénée", 24.0d, 32.0d, "Luxurious" );
		RangeHotel hotel2 = new RangeHotel( 2, "End of the world Hotel - Left", 0.0d, 179.0d, "Roots" );
		RangeHotel hotel3 = new RangeHotel( 3, "End of the world Hotel - Right", 0.0d, -179.0d, "Cosy" );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( hotel );
		fullTextSession.save( hotel2 );
		fullTextSession.save( hotel3 );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude= 24;
		double centerLongitude= 31.5;

		org.apache.lucene.search.Query luceneQuery = SpatialQueryBuilder.buildSpatialQueryByRange(
				centerLatitude, centerLongitude, 50, "location"
		);

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeHotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = SpatialQueryBuilder.buildSpatialQueryByRange(
				centerLatitude, centerLongitude, 51, "location"
		);

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeHotel.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		double endOfTheWorldLatitude= 0.0d;
		double endOfTheWorldLongitude= 180.0d;

		org.apache.lucene.search.Query luceneQuery3 = SpatialQueryBuilder.buildSpatialQueryByRange(
				endOfTheWorldLatitude, endOfTheWorldLongitude, 112, "location"
		);

		org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, RangeHotel.class );
		List results3 = hibQuery3.list();
		Assert.assertEquals( 2, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = SpatialQueryBuilder.buildSpatialQueryByRange(
				endOfTheWorldLatitude, endOfTheWorldLongitude, 100000, "location"
		);

		org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, RangeHotel.class );
		List results4 = hibQuery4.list();
		Assert.assertEquals( 3, results4.size() );

		List<?> events = fullTextSession.createQuery( "from " + RangeHotel.class.getName() ).list();
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
				RangeHotel.class,
				RangeEvent.class
		};
	}
}
