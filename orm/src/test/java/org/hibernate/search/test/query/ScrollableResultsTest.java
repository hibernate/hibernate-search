/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for org.hibernate.search.query.ScrollableResultsImpl
 *
 * @see org.hibernate.search.query.hibernate.impl.ScrollableResultsImpl
 * @author Sanne Grinovero
 */
public class ScrollableResultsTest {

	private FullTextSessionBuilder builder;
	private FullTextSession sess;

	@Before
	public void setUp() {
		builder = new FullTextSessionBuilder();
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

	@After
	public void tearDown() {
		builder.close();
	}

	/**
	 * Test forward scrolling using pagination
	 */
	@Test
	public void testScrollingForward() {
		Transaction tx = sess.beginTransaction();
		TermQuery tq = new TermQuery( new Term( "summary", "number") );
		Sort sort = new Sort( new SortField( "id", SortField.STRING ) );
		ScrollableResults scrollableResults = sess
			.createFullTextQuery( tq, AlternateBook.class )
			.setSort( sort )
			.setFetchSize( 10 )
			.setFirstResult( 20 )
			.setMaxResults( 111 )
			.scroll();
		assertEquals( -1, scrollableResults.getRowNumber() );
		assertTrue( scrollableResults.last() );
		assertEquals( 110, scrollableResults.getRowNumber() );
		scrollableResults.beforeFirst();
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			int bookId = position + 20;
			assertEquals( position, scrollableResults.getRowNumber() );
			AlternateBook book = (AlternateBook) scrollableResults.get()[0];
			assertEquals( bookId, book.getId().intValue() );
			assertEquals( "book about the number " + bookId, book.getSummary() );
			assertTrue( sess.contains( book ) );
		}
		assertEquals( 110, position );
		scrollableResults.close();
		tx.commit();
	}

	/**
	 * Verify inverse-order scrolling.
	 * TODO to verify correct FetchSize behavior I've been debugging
	 * the behavior; we should add a mock library to automate this kind of tests.
	 */
	@Test
	public void testScrollingBackwards() {
		Transaction tx = sess.beginTransaction();
		TermQuery tq = new TermQuery( new Term( "summary", "number") );
		Sort sort = new Sort( new SortField( "id", SortField.STRING ) );
		ScrollableResults scrollableResults = sess
			.createFullTextQuery( tq, AlternateBook.class )
			.setSort( sort )
			.setFetchSize( 10 )
			.scroll();
		scrollableResults.beforeFirst();
		// initial position should be -1 as in Hibernate Core
		assertEquals( -1, scrollableResults.getRowNumber() );
		assertTrue( scrollableResults.last() );
		int position = scrollableResults.getRowNumber();
		assertEquals( 323, position );
		while ( scrollableResults.previous() ) {
			AlternateBook book = (AlternateBook) scrollableResults.get()[0];
			assertEquals( --position, book.getId().intValue() );
			assertEquals( "book about the number " + position, book.getSummary() );
		}
		assertEquals( 0, position );
		assertEquals( -1, scrollableResults.getRowNumber() );
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
		TermQuery tq = new TermQuery( new Term( "summary", "number") );
		Sort sort = new Sort( new SortField( "id", SortField.STRING ) );
		ScrollableResults scrollableResults = sess
			.createFullTextQuery( tq, AlternateBook.class )
			.setSort( sort )
			.setFetchSize( 10 )
			.scroll();
		int position = -1;
		while ( scrollableResults.next() ) {
			position++;
			AlternateBook book = (AlternateBook) scrollableResults.get()[0];
			assertTrue( sess.contains( book ) );
			// evict some entities:
			if ( position % 3 == 0 ) {
				sess.evict( book );
				assertFalse( sess.contains( book ) );
			}
		}
		//verifies it did scroll to the end:
		assertEquals( 323, position );
		//assert the entities are re-attached after eviction:
		while ( scrollableResults.previous() ) {
			position--;
			AlternateBook book = (AlternateBook) scrollableResults.get()[0];
			assertTrue( sess.contains( book ) );
		}
		assertEquals( -1, position );
		sess.clear();
		//assert the entities are re-attached after Session.clear:
		while ( scrollableResults.next() ) {
			position++;
			AlternateBook book = (AlternateBook) scrollableResults.get()[0];
			assertTrue( sess.contains( book ) );
		}
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
		TermQuery tq = new TermQuery( new Term( "dept", "num") );
		//the tests relies on the results being returned sorted by id:
		Sort sort = new Sort( new SortField( "id", SortField.STRING ) );
		ScrollableResults scrollableResults = sess
			.createFullTextQuery( tq, Employee.class )
			.setProjection(
					FullTextQuery.OBJECT_CLASS,
					FullTextQuery.ID,
					FullTextQuery.THIS,
					"lastname",
					FullTextQuery.THIS
					)
			.setFetchSize( 10 )
			.setSort( sort )
			.scroll();
		scrollableResults.last();
		assertEquals( 132, scrollableResults.getRowNumber() );
		scrollableResults.beforeFirst();
		assertEquals( -1, scrollableResults.getRowNumber() );
		int position = scrollableResults.getRowNumber();
		while ( scrollableResults.next() ) {
			position++;
			Object[] objs = scrollableResults.get();
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
		// and now the other way around, checking entities are attached again:
		while ( scrollableResults.previous() ) {
			position--;
			Object[] objs = scrollableResults.get();
			assertTrue( objs[2] instanceof Employee );
			sess.contains( objs[2] );
			assertTrue( objs[4] instanceof Employee );
			sess.contains( objs[4] );
			assertTrue( objs[2] == objs[4] );
		}
		assertEquals( -1, position );
		scrollableResults.close();
		tx.commit();
	}

}
