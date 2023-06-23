/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.spatial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.common.SearchException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.search.Sort;

/**
 * Hibernate Search spatial : unit tests on indexing POIs in with Grid and Grid+Distance
 *
 * @author Nicolas Helleringer
 * @author Hardy Ferentschik
 */
public class SpatialIndexingTest extends SearchTestBase {
	private FullTextSession fullTextSession;

	@Before
	public void createAndIndexTestData() throws Exception {
		fullTextSession = Search.getFullTextSession( openSession() );

		Transaction tx = fullTextSession.beginTransaction();

		// POI
		fullTextSession.save( new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" ) );
		fullTextSession.save( new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" ) );
		fullTextSession.save( new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" ) );
		fullTextSession.save( new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" ) );
		fullTextSession.save( new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" ) );
		fullTextSession.save( new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" ) );

		// NonGeoPOI
		fullTextSession.save( new NonGeoPOI( 1, "Distance to 24,32 : 0", 24.0d, null, "" ) );
		fullTextSession.save( new NonGeoPOI( 2, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" ) );
		fullTextSession.save( new NonGeoPOI( 3, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" ) );
		fullTextSession.save( new NonGeoPOI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" ) );
		fullTextSession.save( new NonGeoPOI( 5, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" ) );
		fullTextSession.save( new NonGeoPOI( 6, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" ) );

		// MissingSpatialPOI
		fullTextSession.save( new MissingSpatialPOI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" ) );

		// Event
		SimpleDateFormat dateFormat = new SimpleDateFormat( "d M yyyy", Locale.ROOT );
		Date date = dateFormat.parse( "10 9 1976" );
		fullTextSession.save( new Event( 1, "Test", 24.0d, 32.0d, date ) );

		// User
		fullTextSession.save( new User( 1, 24.0d, 32.0d ) );

		// UserRange
		fullTextSession.save( new UserRange( 1, 24.0d, 32.0d ) );

		// UserEx
		fullTextSession.save( new UserEx( 1, 24.0d, 32.0d, 11.9d, 27.4d ) );

		// RangeEvent
		dateFormat = new SimpleDateFormat( "d M yyyy", Locale.ROOT );
		date = dateFormat.parse( "10 9 1976" );
		fullTextSession.save( new RangeEvent( 1, "Test", 24.0d, 32.0d, date ) );

		// Hotel
		fullTextSession.save( new Hotel( 1, "Plazza Athénée", 24.0d, 32.0d, "Luxurious" ) );

		// RangeHotel
		fullTextSession.save( new RangeHotel( 1, "Plazza Athénée", 24.0d, 32.0d, "Luxurious" ) );
		fullTextSession.save( new RangeHotel( 2, "End of the world Hotel - Left", 0.0d, 179.0d, "Roots" ) );
		fullTextSession.save( new RangeHotel( 3, "End of the world Hotel - Right", 0.0d, -179.0d, "Cosy" ) );

		// Restaurant
		fullTextSession.save(
				new Restaurant( 1, "Al's kitchen", "42, space avenue CA8596 BYOB Street", 24.0d, 32.0d )
		);

		// GetterUser
		fullTextSession.save( new GetterUser( 1, 24.0d, 32.0d ) );

		//DoubleIndexedPOIs
		fullTextSession.save( new DoubleIndexedPOI( 1, "Davide D'Alto", 37.780392d, -122.513898d, "Hibernate team member" ) );
		fullTextSession.save( new DoubleIndexedPOI( 2, "Peter O'Tall", 40.723165d, -73.987439d, "" ) );

		tx.commit();
	}

	@Test
	@Category(SkipOnElasticsearch.class)
	// Elasticsearch does not support a radius of 0 (starting from 2.2.0)
	public void testIndexingRadius0() throws Exception {
		double centerLatitude = 24;
		double centerLongitude = 32;

		assertNumberOfPointsOfInterestWithinRadius( centerLatitude, centerLongitude, 0, 1 );
	}

	@Test
	public void testIndexing() throws Exception {
		double centerLatitude = 24;
		double centerLongitude = 32;

		assertNumberOfPointsOfInterestWithinRadius( centerLatitude, centerLongitude, 10, 1 );
		assertNumberOfPointsOfInterestWithinRadius( centerLatitude, centerLongitude, 20, 4 );
		assertNumberOfPointsOfInterestWithinRadius( centerLatitude, centerLongitude, 30, 6 );
	}

	@Test
	public void testDistanceProjection() throws Exception {
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder
				.spatial()
				.onField( "location" )
				.within( 100, Unit.KM )
				.ofLatitude( centerLatitude )
				.andLongitude( centerLongitude )
				.createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		hibQuery.setSort( builder.sort().byDistance().onField( "location" ).fromLatitude( centerLatitude )
				.andLongitude( centerLongitude ).createSort() );
		List results = hibQuery.list();
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		Object[] thirdResult = (Object[]) results.get( 2 );
		Object[] fourthResult = (Object[]) results.get( 3 );
		Object[] fifthResult = (Object[]) results.get( 4 );
		Object[] sixthResult = (Object[]) results.get( 5 );
		assertEquals( ( (Double) firstResult[1] ), 0.0, 0.0001 );
		assertEquals( ( (Double) secondResult[1] ), 10.1582, 0.01 );
		assertEquals( ( (Double) thirdResult[1] ), 11.1195, 0.01 );
		assertEquals( ( (Double) fourthResult[1] ), 15.0636, 0.01 );
		assertEquals( ( (Double) fifthResult[1] ), 22.239, 0.02 );
		assertEquals( ( (Double) sixthResult[1] ), 24.446, 0.02 );
	}

	@Test
	public void testDistanceSort() throws Exception {
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = builder.sort().byDistance().onField( "location" )
				.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
				.createSort();
		hibQuery.setSort( distanceSort );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List<Object[]> results = hibQuery.list();

		Double previousDistance = (Double) results.get( 0 )[1];
		for ( int i = 1; i < results.size(); i++ ) {
			Object[] projectionEntry = results.get( i );
			Double currentDistance = (Double) projectionEntry[1];
			assertTrue( previousDistance + " should be < " + currentDistance, previousDistance < currentDistance );
			previousDistance = currentDistance;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1708")
	public void testNonGeoDistanceSortOnNonSpatialField() throws Exception {
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( NonGeoPOI.class ).get();

		try {
			builder.sort().byDistance().onField( "name" )
					.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
					.createSort();
			fail( "Sorting on a field that it is not a coordinate should fail" );
		}
		catch (SearchException e) {
			assertThat( e ).hasMessageContaining( "Cannot use 'sort:distance' on field 'name'" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1708")
	public void testNonGeoDistanceSortOnMissingField() throws Exception {
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( NonGeoPOI.class ).get();

		try {
			builder.sort().byDistance().onField( "location" )
					.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
					.createSort();
			fail( "Sorting on a field not indexed should fail" );
		}
		catch (SearchException e) {
			assertThat( e ).hasMessageContaining( "Unknown field 'location'" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1470")
	public void testSpatialQueryOnNonSpatialConfiguredEntityThrowsException() throws Exception {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( MissingSpatialPOI.class ).get();

		try {
			builder.spatial()
					.within( 1, Unit.KM )
					.ofLatitude( 0d )
					.andLongitude( 0d )
					.createQuery();
			fail( "Building an invalid spatial query should fail" );
		}
		catch (SearchException e) {
			assertThat( e ).hasMessageContaining( "Unknown field '" + Spatial.COORDINATES_DEFAULT_FIELD + "'" );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1470")
	public void testSpatialQueryOnWrongFieldThrowsException() throws Exception {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		try {
			builder.spatial()
					.onField( "foo" )
					.within( 1, Unit.KM )
					.ofLatitude( 0d )
					.andLongitude( 0d )
					.createQuery();
			fail( "Building an invalid spatial query should fail" );
		}
		catch (SearchException e) {
			assertThat( e ).hasMessageContaining( "Unknown field 'foo'" );
		}
	}

	@Test
	public void testSpatialAnnotationOnFieldLevel() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Event.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Event.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Event.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialAnnotationWithSubAnnotationsLevel() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( User.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "home" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, User.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "home" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, User.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialAnnotationWithSubAnnotationsLevelRangeMode() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( UserRange.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial()
				.within( 50, Unit.KM )
				.ofLatitude( centerLatitude )
				.andLongitude( centerLongitude )
				.createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, UserRange.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial()
				.within( 51, Unit.KM )
				.ofLatitude( centerLatitude )
				.andLongitude( centerLongitude )
				.createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, UserRange.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialsAnnotation() throws Exception {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( UserEx.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial()
				.within( 100.0d, Unit.KM )
				.ofLatitude( 24.0d )
				.andLongitude( 31.5d )
				.createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, UserEx.class );
		List results = hibQuery.list();
		assertEquals( 1, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "work" )
				.within( 100.0d, Unit.KM ).ofLatitude( 12.0d ).andLongitude( 27.5d ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, UserEx.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialAnnotationOnFieldLevelRangeMode() throws Exception {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( RangeEvent.class ).get();

		double centerLatitude = 24;
		double centerLongitude = 31.5;

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeEvent.class );


		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeEvent.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialAnnotationOnClassLevel() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Hotel.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "hotel_location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Hotel.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "hotel_location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Hotel.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialAnnotationOnClassLevelRangeMode() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( RangeHotel.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder
				.spatial()
				.within( 50, Unit.KM )
				.ofLatitude( centerLatitude )
				.andLongitude( centerLongitude )
				.createQuery();


		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, RangeHotel.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder
				.spatial()
				.within( 51, Unit.KM )
				.ofLatitude( centerLatitude )
				.andLongitude( centerLongitude )
				.createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, RangeHotel.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );

		double endOfTheWorldLatitude = 0.0d;
		double endOfTheWorldLongitude = 180.0d;

		org.apache.lucene.search.Query luceneQuery3 = builder
				.spatial()
				.within( 112, Unit.KM )
				.ofLatitude( endOfTheWorldLatitude )
				.andLongitude( endOfTheWorldLongitude )
				.createQuery();

		org.hibernate.query.Query hibQuery3 = fullTextSession.createFullTextQuery( luceneQuery3, RangeHotel.class );
		List results3 = hibQuery3.list();
		assertEquals( 2, results3.size() );

		org.apache.lucene.search.Query luceneQuery4 = builder
				.spatial()
				.within( 100000, Unit.KM )
				.ofLatitude( endOfTheWorldLatitude )
				.andLongitude( endOfTheWorldLongitude )
				.createQuery();

		org.hibernate.query.Query hibQuery4 = fullTextSession.createFullTextQuery( luceneQuery4, RangeHotel.class );
		List results4 = hibQuery4.list();
		assertEquals( 3, results4.size() );
	}

	@Test
	public void testSpatialAnnotationOnEmbeddableFieldLevel() throws Exception {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Restaurant.class ).get();

		double centerLatitude = 24;
		double centerLongitude = 31.5;

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "position.location" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, Restaurant.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "position.location" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, Restaurant.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void testSpatialLatLongOnGetters() throws Exception {
		//Point center = Point.fromDegrees( 24, 31.5 ); // 50.79 km fromBoundingCircle 24.32
		double centerLatitude = 24;
		double centerLongitude = 31.5;

		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( GetterUser.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "home" )
				.within( 50, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, GetterUser.class );
		List results = hibQuery.list();
		assertEquals( 0, results.size() );

		org.apache.lucene.search.Query luceneQuery2 = builder.spatial().onField( "home" )
				.within( 51, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery2 = fullTextSession.createFullTextQuery( luceneQuery2, GetterUser.class );
		List results2 = hibQuery2.list();
		assertEquals( 1, results2.size() );
	}

	@Test
	public void test180MeridianCross() throws Exception {

		double centerLatitude = 37.769645d;
		double centerLongitude = -122.446428d;

		final QueryBuilder builder =
				fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( DoubleIndexedPOI.class ).get();

		//Tests with FieldBridge
		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 5000, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		hibQuery.setSort( builder.sort().byDistance().onField( "location" ).fromLatitude( centerLatitude )
				.andLongitude( centerLongitude ).createSort() );
		List results = hibQuery.list();
		assertEquals( 2, results.size() );
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		assertEquals( 6.0492d, (Double) firstResult[1], 0.001 );
		assertEquals( 4132.8166d, (Double) secondResult[1], 1 );

		//Tests with @Longitude+@Latitude
		luceneQuery = builder.spatial()
				.within( 5000, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		hibQuery = fullTextSession.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, Spatial.COORDINATES_DEFAULT_FIELD );
		hibQuery.setSort( builder.sort().byDistance().onField( "location" ).fromLatitude( centerLatitude )
				.andLongitude( centerLongitude ).createSort() );
		results = hibQuery.list();
		assertEquals( 2, results.size() );
		firstResult = (Object[]) results.get( 0 );
		secondResult = (Object[]) results.get( 1 );
		assertEquals( 6.0492d, (Double) firstResult[1], 0.001 );
		assertEquals( 4132.8166d, (Double) secondResult[1], 1 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
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
				GetterUser.class,
				MissingSpatialPOI.class,
				DoubleIndexedPOI.class
		};
	}

	private void assertNumberOfPointsOfInterestWithinRadius(double centerLatitude,
			double centerLongitude,
			double radius,
			int expectedPoiCount) {
		final QueryBuilder builder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( radius, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( luceneQuery, POI.class );
		List results = hibQuery.list();
		assertEquals( "Unexpected number of POIs within radius", expectedPoiCount, results.size() );
	}
}
