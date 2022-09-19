/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.timeout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.QueryTimeoutException;

import org.apache.lucene.search.Query;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.query.SlowQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Emmanuel Bernard
 */
public class JPATimeoutTest extends JPATestCase {

	private Query slowQuery;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		BooleanJunction junction = builder.bool();
		junction.must( builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery() );
		junction.must( new SlowQuery( 10 ) );
		slowQuery = junction.createQuery();
		storeClocks( em );
		em.close();
	}

	@Test
	public void testQueryTimeoutException() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		FullTextQuery hibernateQuery = em.createFullTextQuery( slowQuery, Clock.class );

		hibernateQuery.setHint( "jakarta.persistence.query.timeout", 1 );
		try {
			hibernateQuery.getResultList();
			fail( "timeout exception should happen" );
		}
		catch (QueryTimeoutException e) {
			//good
			e.printStackTrace();
		}
		catch (Exception e) {
			fail( "Expected a QueryTimeoutException" );
		}
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		assertEquals( 100, em.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		em.getTransaction().commit();

		em.close();

	}

	@Test
	public void testLimitFetchingTime() {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );

		em.getTransaction().begin();
		FullTextQuery hibernateQuery = em.createFullTextQuery( slowQuery, Clock.class );
		List results = hibernateQuery.getResultList();
		assertEquals( 50, results.size() );

		em.clear();

		hibernateQuery = em.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 1, TimeUnit.NANOSECONDS );
		List result = hibernateQuery.getResultList();
		System.out.println( "Result size early: " + result.size() );
		assertEquals( "Test early failure, before the number of results are even fetched", 0, result.size() );
		if ( result.size() == 0 ) {
			//sometimes, this
			assertTrue( hibernateQuery.hasPartialResults() );
		}

		em.clear();

		//We cannot test intermediate limit, Lucene / hibernate: too unpredictable

//		hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
//		hibernateQuery.limitFetchingTime( 1000, TimeUnit.NANOSECONDS );
//		results = hibernateQuery.list();
//		System.out.println("Result size partial: " + results.size() );
//		assertTrue("Regular failure when some elements are fetched", 0 < results.size() && results.size() < 500 );
//		assertTrue( hibernateQuery.hasPartialResults() );
//
//		fts.clear();

		hibernateQuery = em.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 30, TimeUnit.SECONDS );
		results = hibernateQuery.getResultList();
		assertEquals( "Test below limit termination", 50, results.size() );
		assertFalse( hibernateQuery.hasPartialResults() );

		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		assertEquals( 100, em.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		em.getTransaction().commit();

		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { Clock.class };
	}

	@Override
	protected void configure(Map cfg) {
		cfg.put( "hibernate.jdbc.batch_size", "1000" );
		super.configure( cfg );
	}

	/**
	 * Use to add some initial data
	 *
	 * @param em the fulltext session
	 */
	private static void storeClocks(FullTextEntityManager em) {
		em.getTransaction().begin();
		for ( int i = 0; i < 100; i++ ) {
			Clock clock = new Clock(
					(long) i,
					"Model cat A" + i,
					( i % 2 == 0 ) ? "Seiko" : "Swatch",
					(long) ( i + 2000 )
			);
			em.persist( clock );
		}
		em.getTransaction().commit();
		em.clear();
	}
}
