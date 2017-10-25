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
import javax.persistence.QueryTimeoutException;

import org.apache.lucene.search.Query;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.testsupport.BytemanHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Emmanuel Bernard
 */
@RunWith(BMUnitRunner.class)
public class JPATimeoutTest extends JPATestCase {

	@Test
	@BMRule(targetClass = "QueryHits",
		targetMethod = "updateTopDocs",
		helper = BytemanHelper.NAME,
		action = "sleepASecond();",
		name = "Enable QueryHits slow down")
	public void testQueryTimeoutException() throws Exception {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		for ( long i = 0; i < 1000; i++ ) {
			Clock clock = new Clock( Long.valueOf( i ), "Model cat A" + i, ( i % 2 == 0 ) ? "Seiko" : "Swatch", Long.valueOf( 2000 + i ) );
			em.persist( clock );
		}
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery();
		FullTextQuery hibernateQuery = em.createFullTextQuery( query, Clock.class );

		hibernateQuery.setHint( "javax.persistence.query.timeout", 100 ); //not too low or we can't reproduce it consistently
		try {
			hibernateQuery.getResultSize();
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
		assertEquals( 1000, em.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		em.getTransaction().commit();

		em.close();

	}

	@Test
	public void testLimitFetchingTime() {
		FullTextEntityManager em = Search.getFullTextEntityManager( factory.createEntityManager() );
		em.getTransaction().begin();
		for ( long i = 0; i < 1000; i++ ) {
			Clock clock = new Clock( Long.valueOf( i ), "Model cat A" + i, ( i % 2 == 0 ) ? "Seiko" : "Swatch", Long.valueOf( 2000 + i ) );
			em.persist( clock );
		}
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		final QueryBuilder builder = em.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery();
		FullTextQuery hibernateQuery = em.createFullTextQuery( query, Clock.class );
		List results = hibernateQuery.getResultList();
		assertEquals( 500, results.size() );

		em.clear();

		query = builder.keyword().onField( "brand" ).matching( "Swatch" ).createQuery();
		hibernateQuery = em.createFullTextQuery( query, Clock.class );
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

//		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
//		hibernateQuery.limitFetchingTime( 1000, TimeUnit.NANOSECONDS );
//		results = hibernateQuery.list();
//		System.out.println("Result size partial: " + results.size() );
//		assertTrue("Regular failure when some elements are fetched", 0 < results.size() && results.size() < 500 );
//		assertTrue( hibernateQuery.hasPartialResults() );
//
//		fts.clear();

		hibernateQuery = em.createFullTextQuery( query, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 30, TimeUnit.SECONDS );
		results = hibernateQuery.getResultList();
		assertEquals( "Test below limit termination", 500, results.size() );
		assertFalse( hibernateQuery.hasPartialResults() );

		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		assertEquals( 1000, em.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
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
}
