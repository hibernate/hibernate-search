/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hibernate.search.test.util.ResourceCleanupFunctions.withinEntityManager;
import static org.hibernate.search.test.util.ResourceCleanupFunctions.withinTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.test.spatial.DoubleIndexedPOI;
import org.hibernate.search.test.spatial.POI;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.After;
import org.junit.Test;

import org.apache.lucene.search.Sort;

/**
 * Hibernate Search spatial : unit tests on quering POIs
 *
 * @author Nicolas Helleringer
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class SpatialQueryingJPATest extends JPATestCase {

	// Use a large, but reasonable distance: Elasticsearch 5 can't handle Double.MAX_VALUE for instance
	private static final double LARGE_DISTANCE_KM = 40_000;

	@After
	public void cleanup() {
		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.createQuery( "from " + POI.class.getName() ).getResultList().forEach( entity -> em.remove( entity ) );
				em.createQuery( "from " + DoubleIndexedPOI.class.getName() ).getResultList()
						.forEach( entity -> em.remove( entity ) );
			} );
		} );
	}

	@Test
	public void testDistanceProjectionWithoutDistanceSort() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi );
				em.persist( poi2 );
				em.persist( poi3 );
			} );
			em.clear();
			//Two different groups so that the query run on multiple segments:
			withinTransaction( em, () -> {
				em.persist( poi4 );
				em.persist( poi5 );
				em.persist( poi6 );
			} );
			em.clear();
			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
						.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				hibQuery.setSort( builder.sort().byField( "idSort" ).createSort() );
				List results = hibQuery.getResultList();
				assertEquals( 6, results.size() );
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				Object[] thirdResult = (Object[]) results.get( 2 );
				Object[] fourthResult = (Object[]) results.get( 3 );
				Object[] fifthResult = (Object[]) results.get( 4 );
				Object[] sixthResult = (Object[]) results.get( 5 );
				assertEquals( 0.0, (Double) firstResult[1], 0.01 );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
				assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
				assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
				assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
				assertEquals( 24.446, (Double) sixthResult[1], 0.02 );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2324")
	public void testDistanceProjectionWithoutDistanceSortMissingCoordinates() throws Exception {
		POI poi1 = new POI( 1, "Distance to 24,32 : unknown (incomplete coordinates)", null, 32d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : unknown (incomplete coordinates)", 24d, null, "" );
		POI poi3 = new POI( 3, "Distance to 23,32 : unknown (incomplete coordinates)", null, null, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi1 );
				em.persist( poi2 );
				em.persist( poi3 );
			} );
			em.clear();
			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
						.within( LARGE_DISTANCE_KM, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				hibQuery.setSort( builder.sort().byField( "idSort" ).createSort() );
				List results = hibQuery.getResultList();
				assertEquals( "Missing coordinates should never appear in spatial query results", 0, results.size() );

				hibQuery = em.createFullTextQuery( builder.all().createQuery(), POI.class );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				hibQuery.setSort( builder.sort().byField( "idSort" ).createSort() );
				results = hibQuery.getResultList();
				assertEquals( 3, results.size() );
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				Object[] thirdResult = (Object[]) results.get( 2 );
				assertNull( firstResult[1] );
				assertNull( secondResult[1] );
				assertNull( thirdResult[1] );
			} );
		} );
	}

	@Test
	public void testDistanceSort() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi );
				em.persist( poi2 );
				em.persist( poi3 );
			} );
			em.clear();
			//Two different groups so that the query run on multiple segments:
			withinTransaction( em, () -> {
				em.persist( poi4 );
				em.persist( poi5 );
				em.persist( poi6 );
			} );
			em.clear();
			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
						.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				Sort distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createSort();
				hibQuery.setSort( distanceSort );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				List results = hibQuery.getResultList();
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				Object[] thirdResult = (Object[]) results.get( 2 );
				Object[] fourthResult = (Object[]) results.get( 3 );
				Object[] fifthResult = (Object[]) results.get( 4 );
				Object[] sixthResult = (Object[]) results.get( 5 );
				assertEquals( 0.0, (Double) firstResult[1], 0.01 );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
				assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
				assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
				assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
				assertEquals( 24.446, (Double) sixthResult[1], 0.02 );

				distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.desc()
						.createSort();
				hibQuery.setSort( distanceSort );
				results = hibQuery.getResultList();
				firstResult = (Object[]) results.get( 0 );
				secondResult = (Object[]) results.get( 1 );
				thirdResult = (Object[]) results.get( 2 );
				fourthResult = (Object[]) results.get( 3 );
				fifthResult = (Object[]) results.get( 4 );
				sixthResult = (Object[]) results.get( 5 );
				assertEquals( 24.446, (Double) firstResult[1], 0.02 );
				assertEquals( 22.239, (Double) secondResult[1], 0.02 );
				assertEquals( 15.0636, (Double) thirdResult[1], 0.01 );
				assertEquals( 11.1195, (Double) fourthResult[1], 0.01 );
				assertEquals( 10.1582, (Double) fifthResult[1], 0.01 );
				assertEquals( 0.0, (Double) sixthResult[1], 0.01 );
			} );
		} );
	}

	@Test
	public void testDistanceSort2() throws Exception {
		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				int cnt = 0;
				for ( double[] c : new double[][] {
						{ 41.04389845, -74.06328534 },
						{ 40.64383333, -73.75050000 },
						{ 40.75666667, -73.98650000 },
						{ 40.69416667, -73.78166667 },
						{ 40.75802992, -73.98532391 },
						{ 40.75802992, -73.98532391 },
						{ 50.77687257, 6.08431213 },
						{ 50.78361600, 6.07003500 },
						{ 50.76066667, 6.08866667 },
						{ 50.77683333, 6.08466667 },
						{ 50.77650000, 6.08416667 },
				} ) {
					em.persist( new POI( cnt, "Test_" + cnt, c[0], c[1], "" ) );
					++cnt;
				}
			} );

			withinTransaction( em, () -> {
				double centerLatitude = 50.7753455;
				double centerLongitude = 6.083886799999959;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
						.within( 1.8097233616663808, Unit.KM )
						.ofLatitude( centerLatitude )
						.andLongitude( centerLongitude )
						.createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				Sort distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createSort();
				hibQuery.setSort( distanceSort );
				hibQuery.setMaxResults( 1000 );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				@SuppressWarnings("unchecked")
				List<Object[]> results = hibQuery.getResultList();

				for ( Object[] result : results ) {
					POI poi = (POI) result[0];
					String message = poi.getName() + " (" + poi.getLatitude() + ", " + poi.getLongitude() + ") is not at "
							+ centerLatitude + ", " + centerLongitude;

					assertThat( message, ( (Double) result[1] ).doubleValue(), is( not( 0.0 ) ) );
				}
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2324")
	public void testDistanceSortMissingCoordinates() throws Exception {
		POI poi = new POI( 0, "Distance to 24,32 : 10.16km", 24.0d, 31.9d, "" );
		POI poi2 = new POI( 1, "Distance to 24,32 : unknown, 4361.00km if interpreted as 0,0", null, null, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi );
				em.persist( poi2 );
			} );
			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.all().createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				Sort distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createSort();
				hibQuery.setSort( distanceSort );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				List results = hibQuery.getResultList();
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				assertEquals( 10.1582, (Double) firstResult[1], 0.01 );
				assertNull( secondResult[1] );

				distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.desc()
						.createSort();
				hibQuery.setSort( distanceSort );
				results = hibQuery.getResultList();
				firstResult = (Object[]) results.get( 0 );
				secondResult = (Object[]) results.get( 1 );
				assertNull( firstResult[1] );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
			} );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2322")
	public void testDistanceSortMissingCoordinatesWholeSegment() throws Exception {
		POI poi = new POI( 0, "Distance to 24,32 : 10.16km", 24.0d, 31.9d, "" );
		POI poi2 = new POI( 1, "Distance to 24,32 : unknown, 4361.00km if interpreted as 0,0", null, null, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> em.persist( poi )
			);

			/*
			 * Create the POI with a missing value in a separate transaction, so that
			 * the document will be alone in one segment (or so it seems...?)
			 */
			withinTransaction( em, () -> em.persist( poi2 )
			);

			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery = builder.all().createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				Sort distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createSort();
				hibQuery.setSort( distanceSort );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				List results = hibQuery.getResultList();
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				assertEquals( 10.1582, (Double) firstResult[1], 0.01 );
				assertNull( secondResult[1] );

				distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.desc()
						.createSort();
				hibQuery.setSort( distanceSort );
				results = hibQuery.getResultList();
				firstResult = (Object[]) results.get( 0 );
				secondResult = (Object[]) results.get( 1 );
				assertNull( firstResult[1] );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
			} );
		} );
	}

	@Test
	public void testDistanceSortWithMaxResult() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi );
				em.persist( poi2 );
				em.persist( poi3 );
			} );
			em.clear();
			//Two different groups so that the query run on multiple segments:
			withinTransaction( em, () -> {
				em.persist( poi4 );
				em.persist( poi5 );
				em.persist( poi6 );
			} );
			em.clear();

			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

				org.apache.lucene.search.Query luceneQuery =
						builder.spatial().onField( "location" ).within( 100, Unit.KM ).ofLatitude( centerLatitude )
								.andLongitude( centerLongitude ).createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
				Sort distanceSort = builder.sort().byDistance().onField( "location" )
						.fromLatitude( centerLatitude ).andLongitude( centerLongitude )
						.createSort();
				hibQuery.setSort( distanceSort );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				// Set max results to 3 when 6 documents are stored:
				hibQuery.setMaxResults( 3 );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				List results = hibQuery.getResultList();
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				Object[] thirdResult = (Object[]) results.get( 2 );
				assertEquals( 0.0, (Double) firstResult[1], 0.01 );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
				assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
			} );
		} );
	}

	@Test
	public void testDoubleIndexedDistanceProjection() throws Exception {
		DoubleIndexedPOI poi1 = new DoubleIndexedPOI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		DoubleIndexedPOI poi2 = new DoubleIndexedPOI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		DoubleIndexedPOI poi3 = new DoubleIndexedPOI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		DoubleIndexedPOI poi4 = new DoubleIndexedPOI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		DoubleIndexedPOI poi5 = new DoubleIndexedPOI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		DoubleIndexedPOI poi6 = new DoubleIndexedPOI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		withinEntityManager( factory, em -> {
			withinTransaction( em, () -> {
				em.persist( poi1 );
				em.persist( poi2 );
				em.persist( poi3 );
			} );
			em.clear();
			//Two different groups so that the query run on multiple segments:
			withinTransaction( em, () -> {
				em.persist( poi4 );
				em.persist( poi5 );
				em.persist( poi6 );
			} );
			em.clear();

			withinTransaction( em, () -> {
				double centerLatitude = 24.0d;
				double centerLongitude = 32.0d;

				final QueryBuilder builder =
						em.getSearchFactory().buildQueryBuilder().forEntity( DoubleIndexedPOI.class ).get();

				//Tests with FieldBridge
				org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
						.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

				FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
				hibQuery.setSort( builder.sort().byField( "idSort" ).createSort() );
				List results = hibQuery.getResultList();
				Object[] firstResult = (Object[]) results.get( 0 );
				Object[] secondResult = (Object[]) results.get( 1 );
				Object[] thirdResult = (Object[]) results.get( 2 );
				Object[] fourthResult = (Object[]) results.get( 3 );
				Object[] fifthResult = (Object[]) results.get( 4 );
				Object[] sixthResult = (Object[]) results.get( 5 );
				assertEquals( 0.0, (Double) firstResult[1], 0.01 );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
				assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
				assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
				assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
				assertEquals( 24.446, (Double) sixthResult[1], 0.02 );

				//Tests with @Latitude+@Longitude
				luceneQuery = builder.spatial()
						.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

				hibQuery = em.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
				hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
				hibQuery.setSpatialParameters( centerLatitude, centerLongitude, Spatial.COORDINATES_DEFAULT_FIELD );
				hibQuery.setSort( builder.sort().byField( "idSort" ).createSort() );
				results = hibQuery.getResultList();
				firstResult = (Object[]) results.get( 0 );
				secondResult = (Object[]) results.get( 1 );
				thirdResult = (Object[]) results.get( 2 );
				fourthResult = (Object[]) results.get( 3 );
				fifthResult = (Object[]) results.get( 4 );
				sixthResult = (Object[]) results.get( 5 );
				assertEquals( 0.0, (Double) firstResult[1], 0.0001 );
				assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
				assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
				assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
				assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
				assertEquals( 24.446, (Double) sixthResult[1], 0.02 );
			} );
		} );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class<?>[] { POI.class, DoubleIndexedPOI.class };
	}
}
