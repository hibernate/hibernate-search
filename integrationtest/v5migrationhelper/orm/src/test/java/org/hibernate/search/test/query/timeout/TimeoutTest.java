/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class TimeoutTest extends SearchTestBase {

	private FullTextSession fts;
	private Query slowQuery;

	@Override
	@BeforeEach
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
	@AfterEach
	public void tearDown() throws Exception {
		try {
			Transaction tx = fts.getTransaction();
			if ( tx.getStatus() != TransactionStatus.ACTIVE ) {
				tx = fts.beginTransaction();
			}
			assertThat( fts.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() ).isEqualTo( 100 );
			fts.purgeAll( Clock.class );
			tx.commit();
			fts.close();
		}
		finally {
			super.tearDown();
		}
	}

	@Test
	void testTimeout() {
		Transaction tx = fts.beginTransaction();

		assertCorrectNumberOfClocksNoTimeout();
		assertTimeoutOccursOnList();
		assertTimeoutOccursOnScroll();

		tx.commit();
	}

	@Test
	void testLimitFetchingTime() {
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
	void testEnoughTime() {
		Transaction tx = fts.beginTransaction();

		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.setTimeout( 5, TimeUnit.MINUTES );
		List results = hibernateQuery.list();
		assertThat( hibernateQuery.hasPartialResults() ).isFalse();
		assertThat( results ).hasSize( 50 );

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
		assertThat( results ).hasSize( 50 );
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
		assertThat( results ).hasSize( 50 ).as( "Test below limit termination" );
		assertThat( hibernateQuery.hasPartialResults() ).isFalse();
		fts.clear();
	}

	private void assertExecutionTimeoutOccursOnList() {
		FullTextQuery hibernateQuery = fts.createFullTextQuery( slowQuery, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 1, TimeUnit.NANOSECONDS );
		List result = hibernateQuery.list();
		System.out.println( "Result size early: " + result.size() );
		assertThat( result ).as( "Test early failure, before the number of results are even fetched" ).isEmpty();
		if ( result.size() == 0 ) {
			//sometimes, this
			assertThat( hibernateQuery.hasPartialResults() ).isTrue();
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
