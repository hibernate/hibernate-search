/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SimpleFieldComparator;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.Author;
import org.hibernate.search.test.query.Book;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
				"title",
				TestConstants.stopAnalyzer
		);

		createTestBooks();
		createTestNumbers();
		createTestContractors();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		// check for ongoing transaction which is an indicator that something went wrong
		// don't call the cleanup methods in this case. Otherwise the original error get swallowed
		if ( fullTextSession.getTransaction().getStatus() != TransactionStatus.ACTIVE ) {
			deleteTestBooks();
			deleteTestNumbers();
			deleteTestContractors();
			fullTextSession.close();
		}
		super.tearDown();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResultOrderedByIdAsString() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "id", SortField.Type.STRING, false ) );
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 10, 2, 3 );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResultOrderedByIdAsLong() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "id_forIntegerSort", SortField.Type.INT, false ) );
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 2, 3, 10 );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	@Category(SkipOnElasticsearch.class)
	public void testResultOrderedByIdAlteringSortStyle() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );

		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING, false ) ) );
		List<Book> result = hibQuery.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 10, 2, 3 );

		hibQuery.setSort( new Sort( new SortField( "id_forIntegerSort", SortField.Type.INT, false ) ) );
		result = hibQuery.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 2, 3, 10 );

		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING, false ) ) );
		result = hibQuery.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 10, 2, 3 );

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
		assertEquals( "Wrong number of test results.", 5, result.size() );
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
		assertEquals( "Wrong number of test results.", 5, result.size() );
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
		Sort sort = new Sort( new SortField( "publicationDate", SortField.Type.STRING, true ) ); //DESC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 4, 10, 3, 2, 1 );
		assertEquals( "Groovy in Action", result.get( 0 ).getSummary() );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	@Category(SkipOnElasticsearch.class)
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
	@Category(SkipOnElasticsearch.class)
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

	@SuppressWarnings("unchecked")
	@Test
	public void testResultOrderedByDocId() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( null, SortField.Type.DOC, false ) );
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsOnly( 1, 2, 3, 10 );

		tx.commit();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testResultOrderedByEmbeddedAuthorNameAscending() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		// order by summary
		Query query = queryParser.parse( "summary:lucene OR summary:action" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		Sort sort = new Sort( new SortField( "mainAuthor.name_sort", SortField.Type.STRING ) ); //ASC
		hibQuery.setSort( sort );
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 2, 1, 3, 4, 10 );

		tx.commit();
	}

	@Test
	public void testSortingByMultipleFields() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "name:Bill OR name:Barny OR name:Bart" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, BrickLayer.class );
		Sort sort = new Sort( new SortField( "sortLastName", SortField.Type.STRING ), new SortField( "sortName", SortField.Type.STRING ) );
		hibQuery.setSort( sort );


		@SuppressWarnings("unchecked")
		List<Book> result = hibQuery.list();
		assertNotNull( result );
		assertThat( result ).onProperty( "lastName" )
			.containsExactly( "Higgins", "Higgins", "Johnson", "Johnson" );

		assertThat( result ).onProperty( "name" )
			.containsExactly( "Barny the brick layer", "Bart the brick layer", "Barny the brick layer", "Bill the brick layer" );

		tx.commit();
	}

	@SuppressWarnings("unchecked")
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2287")
	public void testChangingSortOrder() throws Exception {
		Transaction tx = fullTextSession.beginTransaction();

		Query query = queryParser.parse( "summary:lucene" );
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Book.class );

		hibQuery.setSort( new Sort( new SortField( "id_forIntegerSort", SortField.Type.INT, false ) ) );
		List<Book> result = hibQuery.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 2, 3, 10 );

		hibQuery.setSort( new Sort( new SortField( "id_forIntegerSort", SortField.Type.INT, true ) ) );
		result = hibQuery.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 10, 3, 2, 1 );

		tx.commit();
	}

	/**
	 * Helper method creating three books with the same title and summary.
	 * When searching for these books the results should be returned in the order
	 * they got added to the index.
	 */
	private void createTestBooks() {
		Transaction tx = fullTextSession.beginTransaction();
		Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ROOT );
		cal.set( 2007, Calendar.JULY, 25, 11, 20, 30 );

		Author author = new Author( "Bob" );
		fullTextSession.save( author );
		Book book = new Book( 1, "Hibernate & Lucene", "This is a test book." );
		book.setPublicationDate( cal.getTime() );
		book.setMainAuthor( author );
		fullTextSession.save( book );

		author = new Author( "Anthony" );
		fullTextSession.save( author );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 2, "Hibernate & Lucene", "This is a test book." );
		book.setMainAuthor( author );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );

		author = new Author( "Calvin" );
		fullTextSession.save( author );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 3, "Hibernate & Lucene", "This is a test book." );
		book.setMainAuthor( author );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );

		author = new Author( "Ernst" );
		fullTextSession.save( author );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 10, "Hibernate & Lucene", "This is a test book." );
		book.setMainAuthor( author );
		book.setPublicationDate( cal.getTime() );
		fullTextSession.save( book );

		author = new Author( "Dennis" );
		fullTextSession.save( author );
		cal.add( Calendar.SECOND, 1 );
		book = new Book( 4, "Groovy in Action", "The bible of Groovy" );
		book.setMainAuthor( author );
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

	private void createTestContractors() {
		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.save( new BrickLayer( 2, "Bill the brick layer", "Johnson" ) );
		fullTextSession.save( new BrickLayer( 4, "Barny the brick layer", "Johnson" ) );
		fullTextSession.save( new BrickLayer( 5, "Bart the brick layer", "Higgins" ) );
		fullTextSession.save( new BrickLayer( 6, "Barny the brick layer", "Higgins" ) );

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

	private void deleteTestContractors() {
		Transaction tx = fullTextSession.beginTransaction();
		fullTextSession.createQuery( "delete " + BrickLayer.class.getName() ).executeUpdate();
		tx.commit();
		fullTextSession.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				NumberHolder.class,
				BrickLayer.class
		};
	}

	@Entity
	@Indexed
	@ClassBridge(impl = SortFieldCreatingClassBridge.class)
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

	/**
	 * Class bridge creating doc value fields for custom sorting.
	 *
	 * @author Gunnar Morling
	 */
	public static class SortFieldCreatingClassBridge implements MetadataProvidingFieldBridge {

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			NumberHolder numberHolder = (NumberHolder) value;

			luceneOptions.addNumericDocValuesFieldToDocument( "num1", numberHolder.num1, document );
			luceneOptions.addNumericDocValuesFieldToDocument( "num2", numberHolder.num2, document );
		}

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			builder.field( "sum", FieldType.INTEGER )
				.sortable( true );
		}
	}

	public static class SumFieldComparatorSource extends FieldComparatorSource {
		@Override
		public FieldComparator<?> newComparator(String fieldName, int numHits, int sortPos, boolean reversed)
				throws IOException {
			return new SumFieldComparator( numHits, "num1", "num2" );
		}
	}

	public static class SumFieldComparator extends SimpleFieldComparator<Integer> {
		private final String field1;
		private final String field2;
		private final int[] field1Values;
		private final int[] field2Values;

		private NumericDocValues currentReaderValuesField1;
		private NumericDocValues currentReaderValuesField2;
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
			int v = (int) ( currentReaderValuesField1.get( doc ) + currentReaderValuesField2.get( doc ) );
			return compareValues( bottom, v );
		}

		@Override
		public int compareTop(int doc) throws IOException {
			return topValue - field1Values[doc] - field2Values[doc];
		}

		@Override
		public void copy(int slot, int doc) {
			int v1 = (int) currentReaderValuesField1.get( doc );
			field1Values[slot] = v1;

			int v2 = (int) currentReaderValuesField2.get( doc );
			field2Values[slot] = v2;
		}

		@Override
		protected void doSetNextReader(LeafReaderContext context) throws IOException {
			final LeafReader reader = context.reader();
			currentReaderValuesField1 = reader.getNumericDocValues( field1 );
			currentReaderValuesField2 = reader.getNumericDocValues( field2 );
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
