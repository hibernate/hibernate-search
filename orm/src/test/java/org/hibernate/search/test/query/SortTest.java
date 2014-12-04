/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.Ints;
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
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class SortTest extends SearchTestBase {

	private static FullTextSession fullTextSession;
	private static QueryParser queryParser;

	@Override
	@Before
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
	@After
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
	@Test
	public void testResultOrderedById() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "id", SortField.Type.STRING, false ) );
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
	@Test
	public void testResultOrderedBySummaryStringAscending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by summary
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "summary_forSort", SortField.Type.STRING ) ); //ASC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResultOrderedBySummaryStringDescending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by summary backwards
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "summary_forSort", SortField.Type.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of test results.", 4, result.size() );
		assertEquals( "Hibernate & Lucene", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResultOrderedByDateDescending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by date backwards
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "publicationDate", SortField.Type.LONG, true ) ); //DESC
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
	@Test
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
	@Test
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

		private Ints currentReaderValuesField1;
		private Ints currentReaderValuesField2;
		private int bottom;
		private Integer topValue;

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
			int v = currentReaderValuesField1.get( doc ) + currentReaderValuesField2.get( doc );
			return compareValues( bottom, v );
		}

		@Override
		public int compareTop(int doc) throws IOException {
			return topValue - field1Values[doc] - field2Values[doc];
		}

		@Override
		public void copy(int slot, int doc) {
			int v1 = currentReaderValuesField1.get( doc );
			field1Values[slot] = v1;

			int v2 = currentReaderValuesField2.get( doc );
			field2Values[slot] = v2;
		}

		@Override
		public FieldComparator<Integer> setNextReader(AtomicReaderContext context) throws IOException {
			final AtomicReader reader = context.reader();
			currentReaderValuesField1 = FieldCache.DEFAULT
					.getInts( reader, field1, false );
			currentReaderValuesField2 = FieldCache.DEFAULT
					.getInts( reader, field2, false );
			return this;
		}

		@Override
		public void setBottom(final int bottom) {
			this.bottom = field1Values[bottom] + field2Values[bottom];
		}

		@Override
		public void setTopValue(Integer value) {
			this.topValue = value;
		}

		@Override
		public Integer value(int slot) {
			return field1Values[slot] + field2Values[slot];
		}
	}
}
