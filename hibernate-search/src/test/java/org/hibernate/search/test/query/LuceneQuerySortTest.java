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

import java.util.List;
import java.util.Calendar;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.test.SearchTestCase;


/**
 * @author Hardy Ferentschik
 */
public class LuceneQuerySortTest extends SearchTestCase {

	/**
	 * Test that we can change the default sort order of the lucene search result.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testList() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		createTestBooks(s);
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "title", SearchTestCase.stopAnalyzer );

		Query query = parser.parse( "summary:lucene" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Book.class );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 3, result.size() );
		// make sure that the order is according to in which order the books got inserted
		// into the index.
		int id = 1;
		for(Book b : result) {
			assertEquals( "Expected another id", Integer.valueOf( id ), b.getId() );
			id++;
		}

		// now the same query, but with a lucene sort specified.
		query = parser.parse( "summary:lucene" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "id", SortField.STRING, true ) );
		hibQuery.setSort(sort);
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 3, result.size() );
		id = 3;
		for (Book b : result) {
			assertEquals( "Expected another id", Integer.valueOf( id ), b.getId() );
			id--;
		}

		// order by summary
		query = parser.parse( "summary:lucene OR summary:action" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		sort = new Sort( new SortField( "summary_forSort", SortField.STRING ) ); //ASC
		hibQuery.setSort( sort );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		// order by summary backwards
		query = parser.parse( "summary:lucene OR summary:action" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		sort = new Sort( new SortField( "summary_forSort", SortField.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Hibernate & Lucene", result.get( 0 ).getSummary() );

		// order by date backwards
		query = parser.parse( "summary:lucene OR summary:action" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		sort = new Sort( new SortField( "publicationDate", SortField.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		for (Book book : result) {
			System.out.println(book.getSummary() + " : " + book.getPublicationDate() );
		}
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		tx.commit();

		deleteTestBooks(s);
		s.close();
	}

	/**
	 * Helper method creating three books with the same title and summary.
	 * When searching for these books the results should be returned in the order
	 * they got added to the index.
	 *
	 * @param s The full text session used to index the test data.
	 */
	private void createTestBooks(FullTextSession s) {
		Transaction tx = s.beginTransaction();
		Calendar cal = Calendar.getInstance( );
		cal.set( 2007, 7, 25, 11, 20, 30);
		Book book = new Book(1, "Hibernate & Lucene", "This is a test book.");
		book.setPublicationDate( cal.getTime() );
		s.save(book);
		cal.add( Calendar.SECOND, 1 );
		book = new Book(2, "Hibernate & Lucene", "This is a test book.");
		book.setPublicationDate( cal.getTime() );
		s.save(book);
		cal.add( Calendar.SECOND, 1 );
		book = new Book(3, "Hibernate & Lucene", "This is a test book.");
		book.setPublicationDate( cal.getTime() );
		s.save(book);
		cal.add( Calendar.SECOND, 1 );
		book = new Book(4, "Groovy in Action", "The bible of Groovy");
		book.setPublicationDate( cal.getTime() );
		s.save(book);
		tx.commit();
		s.clear();
	}

	private void deleteTestBooks(FullTextSession s) {
		Transaction tx = s.beginTransaction();
		s.createQuery( "delete " + Book.class.getName() ).executeUpdate();
		tx.commit();
		s.clear();
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class
		};
	}
}
