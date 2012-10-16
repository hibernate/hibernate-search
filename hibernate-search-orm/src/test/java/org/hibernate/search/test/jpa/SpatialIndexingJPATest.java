/**
 * 
 */
package org.hibernate.search.test.jpa;

import java.util.List;

import org.apache.lucene.search.Sort;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.impl.DistanceSortField;
import org.hibernate.search.test.spatial.POI;
import org.junit.Assert;

/**
 * @author Nicolas Helleringer <nicolas.helleringer@novacodex.net>
 * @author Davide Di Somma <davide.disomma@gmail.com>
 *
 */
public class SpatialIndexingJPATest extends JPATestCase {

	/**
	 * 
	 */
	public SpatialIndexingJPATest() {
		super();
	}

	/**
	 * @param name
	 */
	public SpatialIndexingJPATest(String name) {
		super(name);
	}

	public void testDistanceProjection() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2= new POI(  2, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi3= new POI(  3, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi4= new POI(  4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5= new POI(  5, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );
		POI poi6= new POI(  6, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );

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
		double centerLatitude= 24.0d;
		double centerLongitude= 32.0d;

		final QueryBuilder builder = em.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.getResultList();
		Object[] firstResult = (Object[]) results.get(0);
		Object[] secondResult = (Object[]) results.get(1);
		Object[] thirdResult = (Object[]) results.get(2);
		Object[] fourthResult = (Object[]) results.get(3);
		Object[] fifthResult = (Object[]) results.get(4);
		Object[] sixthResult = (Object[]) results.get(5);
		Assert.assertEquals( ((Double)firstResult[1]), 0.0, 0.0001 );
		Assert.assertEquals( ((Double)secondResult[1]), 10.1582, 0.0001 );
		Assert.assertEquals( ((Double)thirdResult[1]), 11.1195, 0.0001 );
		Assert.assertEquals( ((Double)fourthResult[1]), 15.0636, 0.0001 );
		Assert.assertEquals( ((Double)fifthResult[1]), 22.239, 0.001 );
		Assert.assertEquals( ((Double)sixthResult[1]), 24.446, 0.001 );

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for (Object entity : pois) {
			em.remove(entity);
		}
		em.getTransaction().commit();
		em.close();
	}
	
	public void testDistanceSort() throws Exception {
		POI poi = new POI( 1, "Distance to 24,32 : 0", 24.0d, 32.0d, "" );
		POI poi2 = new POI(  2, "Distance to 24,32 : 24.45", 24.2d, 31.9d, "" );
		POI poi3 = new POI(  3, "Distance to 24,32 : 10.16", 24.0d, 31.9d, "" );
		POI poi4 = new POI(  4, "Distance to 24,32 : 15.06", 23.9d, 32.1d, "" );
		POI poi5 = new POI(  5, "Distance to 24,32 : 11.12", 23.9d, 32.0d, "" );
		POI poi6 = new POI(  6, "Distance to 24,32 : 22.24", 24.2d, 32.0d, "" );

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
		double centerLatitude= 24.0d;
		double centerLongitude= 32.0d;

		final QueryBuilder builder = em.getSearchFactory()
				.buildQueryBuilder().forEntity( POI.class ).get();

		org.apache.lucene.search.Query luceneQuery = builder.spatial().onCoordinates( "location" )
				.within( 100, Unit.KM ).ofLatitude( centerLatitude ).andLongitude( centerLongitude ).createQuery();

		FullTextQuery hibQuery = em.createFullTextQuery( luceneQuery, POI.class );
		Sort distanceSort = new Sort( new DistanceSortField( centerLatitude, centerLongitude, "location" ));
		hibQuery.setSort(  distanceSort );
		hibQuery.setProjection( FullTextQuery.THIS, FullTextQuery.SPATIAL_DISTANCE );
		hibQuery.setSpatialParameters( centerLatitude, centerLongitude, "location" );
		List results = hibQuery.getResultList();

//		Object[] firstResult = (Object[]) results.get(0);
//		Object[] secondResult = (Object[]) results.get(1);
//		Object[] thirdResult = (Object[]) results.get(2);
//		Object[] fourthResult = (Object[]) results.get(3);
//		Object[] fifthResult = (Object[]) results.get(4);
//		Object[] sixthResult = (Object[]) results.get(5);
//		Assert.assertEquals( ((Double)firstResult[1]), 0.0, 0.0001 );
//		Assert.assertEquals( ((Double)secondResult[1]), 10.1582, 0.0001 );
//		Assert.assertEquals( ((Double)thirdResult[1]), 11.1195, 0.0001 );
//		Assert.assertEquals( ((Double)fourthResult[1]), 15.0636, 0.0001 );
//		Assert.assertEquals( ((Double)fifthResult[1]), 22.239, 0.001 );
//		Assert.assertEquals( ((Double)sixthResult[1]), 24.446, 0.001 );

		List<?> pois = em.createQuery( "from " + POI.class.getName() ).getResultList();
		for (Object entity : pois) {
			em.remove( entity );
		}
		em.getTransaction().commit();
		em.close();
	}


	/* (non-Javadoc)
	 * @see org.hibernate.search.test.jpa.JPATestCase#getAnnotatedClasses()
	 */
	@Override
	public Class[] getAnnotatedClasses() {
		// TODO Auto-generated method stub
		return new Class<?>[] {
				POI.class
		};
	}

}
