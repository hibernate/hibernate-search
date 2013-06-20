/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.spatial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.lucene.search.Sort;
import org.junit.Assert;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.test.SearchTestCase;

/**
 * Hibernate Search spatial : unit tests on indexing POIs in with Grid and Grid+Distance
 *
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 */
public class SpatialIndexingTest extends SearchTestCase {

	public void testIndexing() throws Exception {
		POI poi = new POI( 1, "Test", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Test2", 0.0d, -179.0d, "" );
		POI poi3 = new POI( 3, "Test3", 0.0d, 179.0d, "" );
		POI poi4 = new POI( 4, "Test4", 89.0d, 1.0d, "" );
		POI poi5 = new POI( 5, "Test5", -90.0d, 17.0d, "" );
		POI poi6 = new POI( 6, "Test6", 47.0d, 154.0d, "" );
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
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, POI.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		org.apache.lucene.search.Query luceneQuery3 = builder.spatial().onCoordinates( "location" )
				.within( 112, Unit.KM ).ofLatitude( 0.0d ).andLongitude( 180.0d ).createQuery();

		org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, POI.class );
		List results3 = hibQuery3.list();
		Assert.assertEquals( 2, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = builder.spatial().onCoordinates( "location" )
				.within( 100000, Unit.KM ).ofLatitude( 0.0d ).andLongitude( 0.0d ).createQuery();

		org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, POI.class );
		List results4 = hibQuery4.list();
		Assert.assertEquals( 6, results4.size() );

		List<?> pois = fullTextSession.createQuery( "from " + POI.class.getName() ).list();
		for ( Object entity : pois ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testDistanceProjection() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi );
		fullTextSession.save( poi2 );
		fullTextSession.save( poi3 );
		tx.commit();
		tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi4 );
		fullTextSession.save( poi5 );
		fullTextSession.save( poi6 );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.list();
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		Object[] thirdResult = (Object[]) results.get( 2 );
		Object[] fourthResult = (Object[]) results.get( 3 );
		Object[] fifthResult = (Object[]) results.get( 4 );
		Object[] sixthResult = (Object[]) results.get( 5 );
		Assert.assertEquals( ((Double)firstResult[1]), 0.0, 0.0001 );
		Assert.assertEquals( ((Double)secondResult[1]), 10.1582, 0.0001 );
		Assert.assertEquals( ((Double)thirdResult[1]), 11.1195, 0.0001 );
		Assert.assertEquals( ((Double)fourthResult[1]), 15.0636, 0.0001 );
		Assert.assertEquals( ((Double)fifthResult[1]), 22.239, 0.001 );
		Assert.assertEquals( ((Double)sixthResult[1]), 24.446, 0.001 );

		List<?> pois = fullTextSession.createQuery( "from " + POI.class.getName() ).list();
		for ( Object entity : pois ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testDistanceSort() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi );
		fullTextSession.save( poi2 );
		fullTextSession.save( poi3 );
		tx.commit();
		tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi4 );
		fullTextSession.save( poi5 );
		fullTextSession.save( poi6 );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ));
		hibQuery.setSort( distanceSort );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.list();

		List<?> pois = fullTextSession.createQuery( "from " + POI.class.getName() ).list();
		for ( Object entity : pois ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testNonGeoDistanceSort() throws Exception {
		NonGeoPOI poi = new NonGeoPOI( 1, "Distance to 24,32 : 0", 24.0d, null, "" );
		NonGeoPOI poi2 = new NonGeoPOI( 2, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );
		NonGeoPOI poi3 = new NonGeoPOI( 3, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		NonGeoPOI poi4 = new NonGeoPOI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		NonGeoPOI poi5 = new NonGeoPOI( 5, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		NonGeoPOI poi6 = new NonGeoPOI( 6, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi );
		fullTextSession.save( poi2 );
		fullTextSession.save( poi3 );
		tx.commit();
		tx = fullTextSession.beginTransaction();
		fullTextSession.save( poi4 );
		fullTextSession.save( poi5 );
		fullTextSession.save( poi6 );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( NonGeoPOI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.all().createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, NonGeoPOI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ));
		hibQuery.setSort( distanceSort );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.list();

		List<?> pois = fullTextSession.createQuery( "from " + NonGeoPOI.class.getName() ).list();
		for ( Object entity : pois ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}


	public void testSpatialAnnotationOnFieldLevel() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("d M yyyy");
		Date date = dateFormat.parse( "10 9 1976" );
		Event event = new Event( 1, "Test", 24.0d, 32.0d, date );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( event );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Event.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Event.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Event.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + Event.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationWithSubAnnotationsLevel() throws Exception {
		User user = new User( 1, 24.0d, 32.0d );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( user );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( User.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "home" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, User.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "home" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, User.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + User.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationWithSubAnnotationsLevelRangeMode() throws Exception {
		UserRange user = new UserRange( 1, 24.0d, 32.0d );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( user );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( UserRange.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onDefaultCoordinates()
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, UserRange.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onDefaultCoordinates()
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, UserRange.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + UserRange.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialsAnnotation() throws Exception {
		UserEx user = new UserEx( 1, 24.0d, 32.0d, 11.9d, 27.4d );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( user );
		tx.commit();

		tx = fullTextSession.beginTransaction();

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( UserEx.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onDefaultCoordinates()
				.within( 100.0d, Unit.KM ).ofLatitude( 24.0d ).andLongitude( 31.5d ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, UserEx.class );
		List results = hibQuery.list();
		Assert.assertEquals( 1, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "work" )
				.within( 100.0d, Unit.KM ).ofLatitude( 12.0d ).andLongitude( 27.5d ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, UserEx.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + UserEx.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationOnFieldLevelRangeMode() throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat( "d M yyyy" );
		Date date = dateFormat.parse( "10 9 1976" );
		RangeEvent event = new RangeEvent( 1, "Test", 24.0d, 32.0d, date );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( event );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( RangeEvent.class ).get();

		double centerLatitude = 24;
		double centerLongitude = 31.5;

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeEvent.class );


		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeEvent.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + RangeEvent.class.getName() ).list();
		for ( Object entity : events ) {
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
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Hotel.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "hotel_location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Hotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "hotel_location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Hotel.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + Hotel.class.getName() ).list();
		for ( Object entity : events ) {
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
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( RangeHotel.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();


		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeHotel.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeHotel.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		double endOfTheWorldLatitude = 0.0d;
		double endOfTheWorldLongitude = 180.0d;

		org.apache.lucene.search.Query luceneQuery3 = builder.spatial().onCoordinates( "location" )
				.within( 112, Unit.KM ).ofLatitude( endOfTheWorldLatitude ).andLongitude( endOfTheWorldLongitude ).createQuery();

		org.hibernate.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, RangeHotel.class );
		List results3 = hibQuery3.list();
		Assert.assertEquals( 2, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = builder.spatial().onCoordinates( "location" )
				.within( 100000, Unit.KM ).ofLatitude( endOfTheWorldLatitude ).andLongitude( endOfTheWorldLongitude ).createQuery();

		org.hibernate.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, RangeHotel.class );
		List results4 = hibQuery4.list();
		Assert.assertEquals( 3, results4.size() );

		List<?> events = fullTextSession.createQuery( "from " + RangeHotel.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialAnnotationOnEmbeddableFieldLevel() throws Exception {
		Restaurant restaurant = new Restaurant( 1, "Al's kitchen", "42, space avenue CA8596 BYOB Street", 24.0d, 32.0d);
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( restaurant );
		tx.commit();

		tx = fullTextSession.beginTransaction();

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Restaurant.class ).get();

		double centerLatitude = 24;
		double centerLongitude = 31.5;

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "position.location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Restaurant.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "position.location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Restaurant.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + Restaurant.class.getName() ).list();
		for ( Object entity : events ) {
			fullTextSession.delete( entity );
		}
		tx.commit();
		fullTextSession.close();
	}

	public void testSpatialLatLongOnGetters() throws Exception {
		GetterUser user = new GetterUser( 1, 24.0d, 32.0d );
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.save( user );
		tx.commit();

		tx = fullTextSession.beginTransaction();
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( GetterUser.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "home" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, GetterUser.class );
		List results = hibQuery.list();
		Assert.assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onCoordinates( "home" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, GetterUser.class );
		List results2 = hibQuery2.list();
		Assert.assertEquals( 1, results2.size() );

		List<?> events = fullTextSession.createQuery( "from " + GetterUser.class.getName() ).list();
		for ( Object entity : events ) {
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
				User.class,
				UserRange.class,
				UserEx.class,
				RangeHotel.class,
				RangeEvent.class,
				Restaurant.class,
				NonGeoPOI.class,
				GetterUser.class
		};
	}
}
