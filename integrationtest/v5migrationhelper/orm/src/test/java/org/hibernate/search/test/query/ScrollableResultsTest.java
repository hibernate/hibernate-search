/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for org.hibernate.search.query.ScrollableResultsImpl
 *
 * @see org.hibernate.search.query.hibernate.impl.ScrollableResultsImpl
 * @author Sanne Grinovero
 */
public class ScrollableResultsTest {

	@Rule
	public FullTextSessionBuilder builder = new FullTextSessionBuilder();

	private FullTextSession sess;

	@Before
	public void setUp() {
		builder
			.addAnnotatedClass( AlternateBook.class )
			.addAnnotatedClass( Employee.class )
			.setProperty( "hibernate.default_batch_fetch_size", "10" )
			.build();
		sess = builder.openFullTextSession();
		Transaction tx = sess.beginTransaction();
		//create some entities to query:
		for ( int i = 0; i < 324; i++ ) {
			sess.persist( new AlternateBook( i , "book about the number " + i ) );
		}
		for ( int i = 0; i < 133; i++ ) {
			sess.persist( new Employee( i , "Rossi", "dept. num. " + i ) );
		}
		tx.commit();
	}

	/**
	 * Test forward scrolling using pagination
	 */
	@Test
	public void testScrollingForward() {
		Transaction tx = sess.beginTransaction();
		QueryBuilder qb = sess.getSearchFactory().buildQueryBuilder().forEntity( AlternateBook.class ).get();
		TermQuery tq = new TermQuery( new Term( "summary", "number" ) );
		Sort sort = qb.sort().byField( "id" ).createSort();
		FullTextQuery query = sess
				.createFullTextQuery( tq, AlternateBook.class )
				.setSort( sort )
				.setFetchSize( 10 )
				.setMaxResults( 111 );
		ScrollableResults scrollableResults = query.scroll();
		assertEquals( -1, scrollableResults.getRowNumber() );
		assertTrue( scrollableResults.last() );
		assertEquals( 110, scrollableResults.getRowNumber() );
		scrollableResults.close();

		scrollableResults = query.scroll();
		scrollableResults.scroll( 20 );
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			int bookId = position;
			assertEquals( position, scrollableResults.getRowNumber() );
			AlternateBook book = (AlternateBook) ( (Object[]) scrollableResults.get() )[0];
			assertEquals( bookId, book.getId().intValue() );
			assertEquals( "book about the number " + bookId, book.getSummary() );
			assertTrue( sess.contains( book ) );
		}
		assertEquals( 110, position );
		scrollableResults.close();
		tx.commit();
	}

	/**
	 * Test that all entities returned by a ScrollableResults
	 * are always attached to Session
	 */
	@Test
	public void testResultsAreManaged() {
		Transaction tx = sess.beginTransaction();
		QueryBuilder qb = sess.getSearchFactory().buildQueryBuilder().forEntity( AlternateBook.class ).get();
		TermQuery tq = new TermQuery( new Term( "summary", "number" ) );
		Sort sort = qb.sort().byField( "id" ).createSort();
		ScrollableResults scrollableResults = sess
			.createFullTextQuery( tq, AlternateBook.class )
			.setSort( sort )
			.setFetchSize( 10 )
			.scroll();
		int position = -1;
		while ( scrollableResults.next() ) {
			position++;
			AlternateBook book = (AlternateBook) ( (Object[]) scrollableResults.get() )[0];
			assertTrue( sess.contains( book ) );
			// evict some entities:
			if ( position % 3 == 0 ) {
				sess.evict( book );
				assertFalse( sess.contains( book ) );
			}
		}
		//verifies it did scroll to the end:
		assertEquals( 323, position );
		tx.commit();
	}

	/**
	 * Verify scrolling works correctly when combined with Projection
	 * and that the projected entities are managed, even in case
	 * of evict usage for memory management.
	 */
	@Test
	public void testScrollProjectionAndManaged() {
		Transaction tx = sess.beginTransaction();
		QueryBuilder qb = sess.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();
		TermQuery tq = new TermQuery( new Term( "dept", "num" ) );
		//the tests relies on the results being returned sorted by id:
		Sort sort = qb.sort().byField( "id" ).createSort();
		FullTextQuery query = sess.createFullTextQuery( tq, Employee.class )
				.setProjection(
						FullTextQuery.OBJECT_CLASS,
						FullTextQuery.ID,
						FullTextQuery.THIS,
						"lastname",
						FullTextQuery.THIS
				)
				.setFetchSize( 10 )
				.setSort( sort );

		ScrollableResults scrollableResults = query.scroll();
		scrollableResults.last();
		assertEquals( 132, scrollableResults.getRowNumber() );
		scrollableResults.close();

		scrollableResults = query.scroll();
		scrollableResults.beforeFirst();
		assertEquals( -1, scrollableResults.getRowNumber() );
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			Object[] objs = (Object[]) scrollableResults.get();
			assertEquals( Employee.class, objs[0] );
			assertEquals( position, objs[1] );
			assertTrue( objs[2] instanceof Employee );
			sess.contains( objs[2] );
			assertEquals( "Rossi", objs[3] );
			assertTrue( objs[4] instanceof Employee );
			sess.contains( objs[4] );
			assertTrue( objs[2] == objs[4] ); // projected twice the same entity
			// detach some objects:
			if ( position % 3 == 0 ) {
				sess.evict( objs[2] );
			}
		}
		//verify we scrolled to the end:
		assertEquals( 132, position );
		scrollableResults.close();
		tx.commit();
	}

}
