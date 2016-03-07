/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.jpa;

import java.util.List;

import org.apache.lucene.search.Sort;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.test.spatial.DoubleIndexedPOI;
import org.hibernate.search.test.spatial.POI;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
/**
 * Hibernate Search spatial : unit tests on quering POIs
 *
 * @author Nicolas Helleringer
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class SpatialQueryingJPATest extends JPATestCase {

	@Test
	public void testDistanceProjection() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		em.persist( poi );
		em.persist( poi2 );
		em.persist( poi3 );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		em.persist( poi4 );
		em.persist( poi5 );
		em.persist( poi6 );
		em.getTransaction().commit();

		em.getTransaction().begin();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.getResultList();
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		Object[] thirdResult = (Object[]) results.get( 2 );
		Object[] fourthResult = (Object[]) results.get( 3 );
		Object[] fifthResult = (Object[]) results.get( 4 );
		Object[] sixthResult = (Object[]) results.get( 5 );
		Assert.assertEquals( 0.0, (Double) firstResult[1], 0.01 );
		Assert.assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
		Assert.assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
		Assert.assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
		Assert.assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
		Assert.assertEquals( 24.446, (Double) sixthResult[1], 0.02 );

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for ( Object entity : pois ) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testDistanceSort() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		em.persist( poi );
		em.persist( poi2 );
		em.persist( poi3 );
		em.getTransaction().commit();
		em.getTransaction().begin();
		em.persist( poi4 );
		em.persist( poi5 );
		em.persist( poi6 );
		em.getTransaction().commit();

		em.getTransaction().begin();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ) );
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
		Assert.assertEquals( 0.0, (Double) firstResult[1], 0.01 );
		Assert.assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
		Assert.assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );
		Assert.assertEquals( 15.0636, (Double) fourthResult[1], 0.01 );
		Assert.assertEquals( 22.239, (Double) fifthResult[1], 0.02 );
		Assert.assertEquals( 24.446, (Double) sixthResult[1], 0.02 );

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for ( Object entity : pois ) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testDistanceSort2() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		int cnt = 0;
		for ( double[] c : new double[][] {
				{41.04389845, -74.06328534},
				{40.64383333, -73.75050000},
				{40.75666667, -73.98650000},
				{40.69416667, -73.78166667},
				{40.75802992, -73.98532391},
				{40.75802992, -73.98532391},
				{50.77687257, 6.08431213},
				{50.78361600, 6.07003500},
				{50.76066667, 6.08866667},
				{50.77683333, 6.08466667},
				{50.77650000, 6.08416667},
		} ) {
			em.persist( new POI( cnt, "Test_" + cnt, c[0], c[1], "" ) );
			++cnt;
		}
		em.getTransaction().commit();


		em.getTransaction().begin();
		double centerLatitude = 50.7753455;
		double centerLongitude = 6.083886799999959;

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 1.8097233616663808, Unit.KM )
					.ofLatitude( centerLatitude )
					.andLongitude( centerLongitude )
				.createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ) );
		hibQuery.setSort( distanceSort );
		hibQuery.setMaxResults( 1000 );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		@SuppressWarnings( "unchecked" )
		List<Object[]> results = hibQuery.getResultList();

		for ( Object[] result : results ) {
			POI poi = (POI)result[0];
			String message = poi.getName() + " (" + poi.getLatitude() + ", " + poi.getLongitude() + ") is not at "
					+ centerLatitude + ", " + centerLongitude;

			Assert.assertThat( message, ( (Double) result[1] ).doubleValue(), is( not( 0.0 ) ) );
		}

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for ( Object entity : pois ) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testDistanceSortWithMaxResult() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3 = new POI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4 = new POI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6 = new POI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		em.persist( poi );
		em.persist( poi2 );
		em.persist( poi3 );
		em.getTransaction().commit();
		em.getTransaction().begin();
		em.persist( poi4 );
		em.persist( poi5 );
		em.persist( poi6 );
		em.getTransaction().commit();

		em.getTransaction().begin();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" ).within( 100, Unit.KM ).ofLatitude( centerLatitude )
				.andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ) );
		hibQuery.setSort( distanceSort );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		// Set max results to 3 when 6 documents are stored:
		hibQuery.setMaxResults( 3 );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.getResultList();
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		Object[] thirdResult = (Object[]) results.get( 2 );
		Assert.assertEquals( 0.0, (Double) firstResult[1], 0.01 );
		Assert.assertEquals( 10.1582, (Double) secondResult[1], 0.01 );
		Assert.assertEquals( 11.1195, (Double) thirdResult[1], 0.01 );

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for ( Object entity : pois ) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testDoubleIndexedDistanceProjection() throws Exception {
		DoubleIndexedPOI poi1 = new DoubleIndexedPOI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		DoubleIndexedPOI poi2 = new DoubleIndexedPOI( 2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		DoubleIndexedPOI poi3 = new DoubleIndexedPOI( 3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		DoubleIndexedPOI poi4 = new DoubleIndexedPOI( 4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		DoubleIndexedPOI poi5 = new DoubleIndexedPOI( 5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		DoubleIndexedPOI poi6 = new DoubleIndexedPOI( 6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		em.persist( poi1 );
		em.persist( poi2 );
		em.persist( poi3 );
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		em.persist( poi4 );
		em.persist( poi5 );
		em.persist( poi6 );
		em.getTransaction().commit();

		em.getTransaction().begin();
		double centerLatitude = 24.0d;
		double centerLongitude = 32.0d;

		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( DoubleIndexedPOI.class ).get();

		//Tests with FieldBridge
		org.apache.lucene.search.Query luceneQuery = builder.spatial().onField( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.getResultList();
		Object[] firstResult = (Object[]) results.get( 0 );
		Object[] secondResult = (Object[]) results.get( 1 );
		Object[] thirdResult = (Object[]) results.get( 2 );
		Object[] fourthResult = (Object[]) results.get( 3 );
		Object[] fifthResult = (Object[]) results.get( 4 );
		Object[] sixthResult = (Object[]) results.get( 5 );
		Assert.assertEquals( (Double) firstResult[1], 0.0, 0.01 );
		Assert.assertEquals( (Double) secondResult[1], 10.1582, 0.01 );
		Assert.assertEquals( (Double) thirdResult[1], 11.1195, 0.01 );
		Assert.assertEquals( (Double) fourthResult[1], 15.0636, 0.01 );
		Assert.assertEquals( (Double) fifthResult[1], 22.239, 0.02 );
		Assert.assertEquals( (Double) sixthResult[1], 24.446, 0.02 );

		//Tests with @Latitude+@Longitude
		luceneQuery = builder.spatial()
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		hibQuery = em.createFullTextQuery( luceneQuery, DoubleIndexedPOI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, Spatial.COORDINATES_DEFAULT_FIELD );
		results = hibQuery.getResultList();
		firstResult = (Object[]) results.get( 0 );
		secondResult = (Object[]) results.get( 1 );
		thirdResult = (Object[]) results.get( 2 );
		fourthResult = (Object[]) results.get( 3 );
		fifthResult = (Object[]) results.get( 4 );
		sixthResult = (Object[]) results.get( 5 );
		Assert.assertEquals( (Double) firstResult[1], 0.0, 0.0001 );
		Assert.assertEquals( (Double) secondResult[1], 10.1582, 0.01 );
		Assert.assertEquals( (Double) thirdResult[1], 11.1195, 0.01 );
		Assert.assertEquals( (Double) fourthResult[1], 15.0636, 0.01 );
		Assert.assertEquals( (Double) fifthResult[1], 22.239, 0.02 );
		Assert.assertEquals( (Double) sixthResult[1], 24.446, 0.02 );


		List<?> pois = em.createQuery( "from " + DoubleIndexedPOI.class.getName() ).getResultList();
		for ( Object entity : pois ) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class<?>[] { POI.class, DoubleIndexedPOI.class };
	}
}
