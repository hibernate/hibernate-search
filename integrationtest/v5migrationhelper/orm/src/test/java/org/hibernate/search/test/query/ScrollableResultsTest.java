/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.util.FullTextSessionBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;

/**
 * Test for org.hibernate.search.query.ScrollableResultsImpl
 *
 * @see org.hibernate.internal.ScrollableResultsImpl
 * @author Sanne Grinovero
 */
class ScrollableResultsTest {

	@RegisterExtension
	public FullTextSessionBuilder builder = new FullTextSessionBuilder();

	private FullTextSession sess;

	@BeforeEach
	void setUp() {
		builder
				.addAnnotatedClass( AlternateBook.class )
				.addAnnotatedClass( Employee.class )
				.setProperty( "hibernate.default_batch_fetch_size", "10" )
				.build();
		sess = builder.openFullTextSession();
		Transaction tx = sess.beginTransaction();
		//create some entities to query:
		for ( int i = 0; i < 324; i++ ) {
			sess.persist( new AlternateBook( i, "book about the number " + i ) );
		}
		for ( int i = 0; i < 133; i++ ) {
			sess.persist( new Employee( i, "Rossi", "dept. num. " + i ) );
		}
		tx.commit();
	}

	/**
	 * Test forward scrolling using pagination
	 */
	@Test
	void testScrollingForward() {
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
		assertThat( scrollableResults.getRowNumber() ).isEqualTo( -1 );
		assertThat( scrollableResults.last() ).isTrue();
		assertThat( scrollableResults.getRowNumber() ).isEqualTo( 110 );
		scrollableResults.close();

		scrollableResults = query.scroll();
		scrollableResults.scroll( 20 );
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			int bookId = position;
			assertThat( scrollableResults.getRowNumber() ).isEqualTo( position );
			AlternateBook book = (AlternateBook) ( (Object[]) scrollableResults.get() )[0];
			assertThat( book.getId().intValue() ).isEqualTo( bookId );
			assertThat( book.getSummary() ).isEqualTo( "book about the number " + bookId );

			assertThat( sess.contains( book ) ).isTrue();
		}
		assertThat( position ).isEqualTo( 110 );
		scrollableResults.close();
		tx.commit();
	}

	/**
	 * Test that all entities returned by a ScrollableResults
	 * are always attached to Session
	 */
	@Test
	void testResultsAreManaged() {
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
			assertThat( sess.contains( book ) ).isTrue();
			// evict some entities:
			if ( position % 3 == 0 ) {
				sess.evict( book );
				assertThat( sess.contains( book ) ).isFalse();
			}
		}
		//verifies it did scroll to the end:
		assertThat( position ).isEqualTo( 323 );
		tx.commit();
	}

	/**
	 * Verify scrolling works correctly when combined with Projection
	 * and that the projected entities are managed, even in case
	 * of evict usage for memory management.
	 */
	@Test
	void testScrollProjectionAndManaged() {
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
		assertThat( scrollableResults.getRowNumber() ).isEqualTo( 132 );
		scrollableResults.close();

		scrollableResults = query.scroll();
		scrollableResults.beforeFirst();
		assertThat( scrollableResults.getRowNumber() ).isEqualTo( -1 );
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			Object[] objs = (Object[]) scrollableResults.get();
			assertThat( objs[0] ).isEqualTo( Employee.class );
			assertThat( objs[1] ).isEqualTo( position );
			assertThat( objs[2] instanceof Employee ).isTrue();
			sess.contains( objs[2] );
			assertThat( objs[3] ).isEqualTo( "Rossi" );
			assertThat( objs[4] instanceof Employee ).isTrue();
			sess.contains( objs[4] );
			assertThat( objs[2] == objs[4] ).isTrue(); // projected twice the same entity
			// detach some objects:
			if ( position % 3 == 0 ) {
				sess.evict( objs[2] );
			}
		}
		//verify we scrolled to the end:
		assertThat( position ).isEqualTo( 132 );
		scrollableResults.close();
		tx.commit();
	}

}
