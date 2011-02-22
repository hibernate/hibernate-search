package org.hibernate.search.test.query.timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.search.Query;

import org.hibernate.QueryTimeoutException;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class TimeoutTest extends SearchTestCase {

	public void testTimeout() {
		FullTextSession fts = Search.getFullTextSession( openSession(  ) );
		storeClocks( fts );

		Transaction tx = fts.beginTransaction();
		final QueryBuilder builder = fts.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery();
		FullTextQuery hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		final List results = hibernateQuery.list();
		assertEquals( 500, results.size() );

		fts.clear();

		hibernateQuery.setTimeout( 10, TimeUnit.MICROSECONDS );
		long start = System.nanoTime();
		try {
			hibernateQuery.list();
			fail("timeout exception should happen");
		}
		catch ( QueryTimeoutException e ) {
			//good
		}
		catch ( Exception e ) {
			fail("Expected a QueryTimeoutException");
		}

		fts.clear();
		query = builder.keyword().onField( "brand" ).matching( "Swatch" ).createQuery();
		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		hibernateQuery.setTimeout( 10, TimeUnit.MICROSECONDS );

		try {
			hibernateQuery.iterate();
			fail("timeout exception should happen");
		}
		catch ( QueryTimeoutException e ) {
			//good
		}
		catch ( Exception e ) {
			fail("Expected a QueryTimeoutException");
		}

		fts.clear();
		query = builder.keyword().onField( "brand" ).matching( "Blah" ).createQuery();
		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		hibernateQuery.setTimeout( 10, TimeUnit.MICROSECONDS );
		
		try {
			hibernateQuery.scroll();
			fail("timeout exception should happen");
		}
		catch ( QueryTimeoutException e ) {
			//good
		}
		catch ( Exception e ) {
			fail("Expected a QueryTimeoutException");
		}
		System.out.println("Time = " + (System.nanoTime() - start));

		tx.commit();

		fts.clear();

		tx = fts.beginTransaction();
		assertEquals( 1000, fts.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		tx.commit();

		fts.close();

	}

	public void testLimitFetchingTime() {
		FullTextSession fts = Search.getFullTextSession( openSession(  ) );
		storeClocks( fts );

		Transaction tx = fts.beginTransaction();

		final QueryBuilder builder = fts.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.keyword().onField( "brand" ).matching( "Seiko" ).createQuery();
		FullTextQuery hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		List results = hibernateQuery.list();
		assertEquals( 500, results.size() );

		fts.clear();


		query = builder.keyword().onField( "brand" ).matching( "Swatch" ).createQuery();
		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 1, TimeUnit.NANOSECONDS );
		List result = hibernateQuery.list();
		System.out.println("Result size early: " + result.size() );
		assertEquals("Test early failure, before the number of results are even fetched", 0, result.size() );
		if ( result.size() == 0) {
			//sometimes, this
			assertTrue( hibernateQuery.hasPartialResults() );
		}

		fts.clear();

		//We cannot test intermediate limit, Lucene / hibernate: too unpredictable

//		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
//		hibernateQuery.limitFetchingTime( 1000, TimeUnit.NANOSECONDS );
//		results = hibernateQuery.list();
//		System.out.println("Result size partial: " + results.size() );
//		assertTrue("Regular failure when some elements are fetched", 0 < results.size() && results.size() < 500 );
//		assertTrue( hibernateQuery.hasPartialResults() );
//
//		fts.clear();

		hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		hibernateQuery.limitExecutionTimeTo( 30, TimeUnit.SECONDS );
		results = hibernateQuery.list();
		assertEquals("Test below limit termination", 500, results.size() );
		assertFalse( hibernateQuery.hasPartialResults() );

		tx.commit();

		fts.clear();

		tx = fts.beginTransaction();
		assertEquals( 1000, fts.createQuery( "delete from " + Clock.class.getName() ).executeUpdate() );
		tx.commit();

		fts.close();

	}
	
	public void testEnoughTime() {
		FullTextSession fts = Search.getFullTextSession( openSession(  ) );
		storeClocks( fts );

		Transaction tx = fts.beginTransaction();

		final QueryBuilder builder = fts.getSearchFactory().buildQueryBuilder().forEntity( Clock.class ).get();
		Query query = builder.all().createQuery();
		FullTextQuery hibernateQuery = fts.createFullTextQuery( query, Clock.class );
		hibernateQuery.setTimeout( 5, TimeUnit.MINUTES );
		List results = hibernateQuery.list();
		assertFalse( hibernateQuery.hasPartialResults() );
		assertEquals( 1000, results.size() );
		
		tx.commit();
		fts.close();
	}
	
	/**
	 * Use to add some initial data
	 */
	private void storeClocks(FullTextSession fts) {
		Transaction tx = fts.beginTransaction();
		for ( int i = 0 ; i < 1000 ; i++ ) {
			Clock clock  = new Clock("Model cat A" + i, (i % 2 == 0) ? "Seiko" : "Swatch", new Long (2000 + i) ) ;
			fts.persist( clock );
		}
		tx.commit();
		fts.clear();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Clock.class };
	}
}
