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

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Hardy Ferentschik
 */
public class SortTest extends SearchTestCase {

	private static FullTextSession fullTextSession;
	private static QueryParser queryParser;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fullTextSession = Search.getFullTextSession( openSession() );
		queryParser = new QueryParser(
				TestConstants.getTargetLuceneVersion(),
				"title",
				TestConstants.stopAnalyzer
		);

		createTestBooks();
		createTestNumbers();
	}

	@Override
	public void tearDown() throws Exception {
		// check for ongoing transaction which is an indicator that something went wrong
		// don't call the cleanup methods in this case. Otherwise the original error get swallowed
		if ( !fullTextSession.getTransaction().isActive() ) {
			deleteTestBooks();
			deleteTestNumbers();
			fullTextSession.close();
		}
		super.tearDown();
	}

	@SuppressWarnings("unchecked")
	public void testResultOrderedById() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "id", SortField.STRING, false ) );
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 3, result.size() );
		int id = 1;
		for ( Book b : result ) {
			assertEquals( "Expected another id", Integer.valueOf( id ), b.getId() );
			id++;
		}

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	public void testResultOrderedBySummaryStringAscending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by summary
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "summary_forSort", SortField.STRING ) ); //ASC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	public void testResultOrderedBySummaryStringDescending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by summary backwards
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "summary_forSort", SortField.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Hibernate & Lucene", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	public void testResultOrderedByDateDescending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by date backwards
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "publicationDate", SortField.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		for ( Book book : result ) {
			System.out.println( book.getSummary() + " : " + book.getPublicationDate() );
		}
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	public void testCustomFieldComparatorAscendingSort() {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = new MatchAllDocsQuery();
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, NumberHolder.class );
		Sort sort = new Sort( new SortField( "sum", new SumFieldComparatorSource() ) );
		hibQuery.setSort( sort );
		List<NumberHolder> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );

		int previousSum = 0;
		for ( NumberHolder n : result ) {
			assertTrue( "Documents should be ordered by increasing sum", previousSum < n.getSum() );
			previousSum = n.getSum();
		}

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	public void testCustomFieldComparatorDescendingSort() {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = new MatchAllDocsQuery();
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, NumberHolder.class );
		Sort sort = new Sort( new SortField( "sum", new SumFieldComparatorSource(), true ) );
		hibQuery.setSort( sort );
		List<NumberHolder> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );

		int previousSum = 100;
		for ( NumberHolder n : result ) {
			assertTrue( "Documents should be ordered by decreasing sum", previousSum > n.getSum() );
			previousSum = n.getSum();
		}

		tx.commit();
	}

	/**
	 * Helper method creating three books with the same title and summary.
	 * When searching for these books the results should be returned in the order
	 * they got added to the index.
	 */
	private void createTestBooks() {
		Transaction tx = fullTextSession.beginTransaction();
		Calendar cal = Calendar.getInstance();
		cal.set( 2007, Calendar.JULY, 25, 11, 20, 30 );
		Book book = new Book( 1, "Hibernate & Lucene", "This is a test book." );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 2, "Hibernate & Lucene", "This is a test book." );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 3, "Hibernate & Lucene", "This is a test book." );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 4, "Groovy in Action", "The bible of Groovy" );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );
		tx.commit();
		fullTextSession.clear();
	}

	/**
	 * Helper method creating test data for number holder.
	 */
	private void createTestNumbers() {
		Transaction tx = fullTextSession.beginTransaction();
		NumberHolder holder = new NumberHolder( 1, 1 );
		fullTextSession.save( holder );
		holder = new NumberHolder( 1, 10 );
		fullTextSession.save( holder );
		holder = new NumberHolder( 1, 5 );
		fullTextSession.save( holder );
		holder = new NumberHolder( 3, 2 );
		fullTextSession.save( holder );
		tx.commit();
		fullTextSession.clear();
	}

	private void deleteTestBooks() {
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.createQuery( "delete " + Book.class.getName() ).executeUpdate();
		tx.commit();
		fullTextSession.clear();
	}

	private void deleteTestNumbers() {
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.createQuery( "delete " + NumberHolder.class.getName() ).executeUpdate();
		tx.commit();
		fullTextSession.clear();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				NumberHolder.class
		};
	}

	@Entity
	@Indexed
	public static class NumberHolder {
		@Id
		@GeneratedValue
		int id;

		@Field(analyze = Analyze.NO)
		int num1;

		@Field(analyze = Analyze.NO)
		int num2;

		public NumberHolder(int num1, int num2) {
			this.num1 = num1;
			this.num2 = num2;
		}

		public NumberHolder() {
		}

		public int getSum() {
			return num1 + num2;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append( "NumbersHolder" );
			sb.append( "{id=" ).append( id );
			sb.append( ", num1=" ).append( num1 );
			sb.append( ", num2=" ).append( num2 );
			sb.append( '}' );
			return sb.toString();
		}
	}

	public static class SumFieldComparatorSource extends FieldComparatorSource {
		@Override
		public FieldComparator<?> newComparator(String fieldName, int numHits, int sortPos, boolean reversed)
				throws IOException {
			return new SumFieldComparator( numHits, "num1", "num2" );
		}
	}

	public static class SumFieldComparator extends FieldComparator<Integer> {
		private final String field1;
		private final String field2;
		private final int[] field1Values;
		private final int[] field2Values;

		private int[] currentReaderValuesField1;
		private int[] currentReaderValuesField2;
		private int bottom;


		public SumFieldComparator(int numHits, String field1, String field2) {
			this.field1 = field1;
			this.field2 = field2;
			this.field1Values = new int[numHits];
			this.field2Values = new int[numHits];
		}

		@Override
		public int compare(int slot1, int slot2) {
			final int v1 = field1Values[slot1] + field2Values[slot1];
			final int v2 = field1Values[slot2] + field2Values[slot2];

			return compareValues( v1, v2 );
		}


		private int compareValues(int v1, int v2) {
			if ( v1 > v2 ) {
				return 1;
			}
			else if ( v1 < v2 ) {
				return -1;
			}
			else {
				return 0;
			}
		}

		@Override
		public int compareBottom(int doc) {
			int v = currentReaderValuesField1[doc] + currentReaderValuesField2[doc];
			return compareValues( bottom, v );
		}

		@Override
		public void copy(int slot, int doc) {
			int v1 = currentReaderValuesField1[doc];
			field1Values[slot] = v1;

			int v2 = currentReaderValuesField2[doc];
			field2Values[slot] = v2;
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase) throws IOException {
			currentReaderValuesField1 = FieldCache.DEFAULT
					.getInts( reader, field1, FieldCache.DEFAULT_INT_PARSER, false );
			currentReaderValuesField2 = FieldCache.DEFAULT
					.getInts( reader, field2, FieldCache.DEFAULT_INT_PARSER, false );
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = field1Values[bottom] + field2Values[bottom];
		}

		@Override
		public Integer value(int slot) {
			return field1Values[slot] + field2Values[slot];
		}
	}
}
