/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.timeout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.QueryTimeoutException;

import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.query.SlowQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutTest extends SearchTestBase {

	private FullTextSession fts;
	private Query slowQuery;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fts = Search.getFullTextSession( openSession() );
		QueryBuilder builder = fts.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		BooleanJunction junction = builder.bool();
		junction.must( builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery() );
		junction.must( new SlowQuery( 10 ) );
		slowQuery = junction.createQuery();
		storeClocks( fts );
	}

	@Override
	@After
	public void tearDown() throws Exception {
		try {
			Transaction tx = fts.getTransaction();
			if ( tx.getStatus() != TransactionStatus.ACTIVE ) {
				tx = fts.beginTransaction();
			}
			assertEquals( 100, fts.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
			fts.purgeAll( Clock.class );
			tx.commit();
			fts.close();
		}
		finally {
			super.tearDown();
		}
	}

	@Test
	public void testTimeout() {
		Transaction tx = fts.beginTransaction();

		assertCorrectNumberOfClocksNoTimeout();
		assertTimeoutOccursOnList();
		assertTimeoutOccursOnScroll();

		tx.commit();
	}

	@Test
	public void testLimitFetchingTime() {
		Transaction tx = fts.beginTransaction();

		assertCorrectNumberOfClocksNoTimeout();
		assertExecutionTimeoutOccursOnList();
		assertExecutionTimeoutHasNoPartialResult();

		tx.commit();

		//We cannot test intermediate limit, Lucene / hibernate: too unpredictable

		//		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		//		hibernateQuery.limitFetchingTime( 1000, TimeUnit.NANOSECONDS );
		//		results = hibernateQuery.list();
		//		System.out.println("Result size partial: " + results.size() );
		//		assertTrue("Regular failure when some elements are fetched", 0 < results.size() && results.size() < 500 );
		//		assertTrue( hibernateQuery.hasPartialResults() );
		//
		//		fts.clear();
	}

	@Test
	public void testEnoughTime() {
		Transaction tx = fts.beginTransaction();

		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.setTimeout( 5, TimeUnit.MINUTES );
		List results = hibernateQuery.list();
		assertFalse( hibernateQuery.hasPartialResults() );
		assertEquals( 50, results.size() );

		tx.commit();
	}

	private void assertTimeoutOccursOnScroll() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.setTimeout( 10, TimeUnit.MICROSECONDS );
		hibernateQuery.setFetchSize( 100 );
		try {
			ScrollableResults scroll = hibernateQuery.scroll();
			scroll.next();
			fail( "timeout exception should happen" );
		}
		catch (QueryTimeoutException e) {
			//good
		}
		catch (Exception e) {
			fail( "Expected a QueryTimeoutException" );
		}
		fts.clear();
	}

	private void assertTimeoutOccursOnList() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.setTimeout( 10, TimeUnit.MICROSECONDS );
		try {
			hibernateQuery.list();
			fail( "timeout exception should happen" );
		}
		catch (QueryTimeoutException e) {
			//good
		}
		catch (Exception e) {
			fail( "Expected a QueryTimeoutException" );
		}
		fts.clear();
	}

	private void assertCorrectNumberOfClocksNoTimeout() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		final List results = hibernateQuery.list();
		assertEquals( 50, results.size() );
		fts.clear();
	}

	/**
	 * Use to add some initial data
	 *
	 * @param fts the fulltext session
	 */
	private static void storeClocks(FullTextSession fts) {
		Transaction tx = fts.beginTransaction();
		for ( int i = 0; i < 100; i++ ) {
			Clock clock = new Clock(
					(long) i,
					"Model cat A" + i,
					( i % 2 == 0 ) ? "Seiko" : "Swatch",
					(long) ( i + 2000 )
			);
			fts.persist( clock );
		}
		tx.commit();
		fts.clear();
	}

	private void assertExecutionTimeoutHasNoPartialResult() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 30, TimeUnit.SECONDS );
		List results = hibernateQuery.list();
		assertEquals( "Test below limit termination", 50, results.size() );
		assertFalse( hibernateQuery.hasPartialResults() );
		fts.clear();
	}

	private void assertExecutionTimeoutOccursOnList() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 1, TimeUnit.NANOSECONDS );
		List result = hibernateQuery.list();
		System.out.println( "Result size early: " + result.size() );
		assertEquals( "Test early failure, before the number of results are even fetched", 0, result.size() );
		if ( result.size() == 0 ) {
			//sometimes, this
			assertTrue( hibernateQuery.hasPartialResults() );
		}
		fts.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Clock.class };
	}

	@Override
	public void configure(Map<String, Object> cfg) {
		cfg.put( "hibernate.jdbc.batch_size", "1000" );
	}

}
