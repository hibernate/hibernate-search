/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.impl.DeleteByQuerySupport;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.DeletionQuery;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Martin Braun
 */
public class DeleteByQueryTest {

	private static final IndexedTypeIdentifier BOOK_TYPE = new PojoIndexedTypeIdentifier( Book.class );
	private static final IndexedTypeIdentifier MOVIE_TYPE = new PojoIndexedTypeIdentifier( Movie.class );

	@Rule
	public final SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Book.class, Movie.class );

	private final SearchITHelper helper = new SearchITHelper( factoryHolder );

	@Test
	public void testStringSerialization() {
		for ( DeletionQuery q : this.buildQueries() ) {
			this.testSerializationForQuery( q );
		}
	}

	private List<DeletionQuery> buildQueries() {
		List<DeletionQuery> l = new ArrayList<>();

		l.add( new SingularTermDeletionQuery( "id", "123" ) );
		l.addAll( this.buildNumQueries() );

		return l;
	}

	private List<SingularTermDeletionQuery> buildNumQueries() {
		List<SingularTermDeletionQuery> l = new ArrayList<>();

		{
			l.add( new SingularTermDeletionQuery( "intField", 1 ) );
			l.add( new SingularTermDeletionQuery( "longField", 1L ) );
			l.add( new SingularTermDeletionQuery( "floatField", 1F ) );
			l.add( new SingularTermDeletionQuery( "doubleField", 1D ) );
		}

		return l;
	}

	private List<Integer> expectedCount() {
		List<Integer> l = new ArrayList<>();
		l.add( 1 );
		l.add( 1 );
		l.add( 1 );
		l.add( 1 );
		return l;
	}

	@Test
	public void testSingularTermQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();
		Worker worker = integrator.getWorker();

		makeBooksForSingularTermQuery();
		this.assertCount( Book.class, 2 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( BOOK_TYPE, new SingularTermDeletionQuery( "url", "lordoftherings" ) ), tc );
			tc.end();
		}
		this.assertCount( Book.class, 1 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( BOOK_TYPE, new SingularTermDeletionQuery( "url", "thehobbit" ) ), tc );
			tc.end();
		}
		this.assertCount( Book.class, 0 );
		// this should stay empty now!
	}

	@Test
	public void testStringIdTermQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();
		Worker worker = integrator.getWorker();

		makeBooksForSingularTermQuery();
		this.assertCount( Book.class, 2 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( BOOK_TYPE, new SingularTermDeletionQuery( "id", String.valueOf( 5 ) ) ), tc );
			tc.end();
		}
		this.assertCount( Book.class, 1 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( BOOK_TYPE, new SingularTermDeletionQuery( "id", String.valueOf( 6 ) ) ), tc );
			tc.end();
		}
		this.assertCount( Book.class, 0 );
		// this should stay empty now!
	}

	@Test
	public void testNumericIdTermQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();
		Worker worker = integrator.getWorker();

		makeMoviesForNumericIdTermQuery();
		this.assertCount( Movie.class, 2 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( MOVIE_TYPE, new SingularTermDeletionQuery( "id", 3 ) ), tc );
			tc.end();
		}
		this.assertCount( Movie.class, 1 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( MOVIE_TYPE, new SingularTermDeletionQuery( "id", 4 ) ), tc );
			tc.end();
		}
		this.assertCount( Movie.class, 0 );
		// this should stay empty now!
	}

	@Test
	public void testNumRangeQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();

		List<SingularTermDeletionQuery> numQueries = this.buildNumQueries();
		List<Integer> expectedCount = this.expectedCount();
		assertEquals( expectedCount.size(), numQueries.size() );

		for ( int i = 0; i < numQueries.size(); ++i ) {
			try {
				this.testForQuery( Book.class, integrator, numQueries.get( i ), expectedCount.get( i ) );
			}
			catch (Throwable e) {
				System.out.println( "ERROR: " + numQueries.get( i ) + ". expected was: " + expectedCount.get( i ) );
				throw e;
			}
		}
	}

	private void testForQuery(Class<?> entityType, ExtendedSearchIntegrator integrator, DeletionQuery query, int expectedCount) {
		Worker worker = integrator.getWorker();

		makeBooksForNumRangeQuery();
		this.assertCount( entityType, 2 );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( BOOK_TYPE, query ), tc );
			tc.end();
		}
		this.assertCount( entityType, expectedCount );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new Work( BOOK_TYPE, null, WorkType.PURGE_ALL ), tc );
			tc.end();
		}
		this.assertCount( entityType, 0 );
	}

	private void assertCount(Class<?> entityType, int count) {
		helper.assertThat().from( entityType ).hasResultSize( count );
	}

	private void testSerializationForQuery(DeletionQuery deletionQuery) {
		assertTrue( DeleteByQuerySupport.isSupported( deletionQuery.getClass() ) );
		String[] strRep = deletionQuery.serialize();
		DeletionQuery copy = DeleteByQuerySupport.fromString( deletionQuery.getQueryKey(), strRep );
		assertEquals( deletionQuery, copy );
	}

	private void makeBooksForSingularTermQuery() {
		// just some random data:
		helper.add(
				new Book( String.valueOf( 5 ), "Lord of The Rings", "lordoftherings" ),
				new Book( String.valueOf( 6 ), "The Hobbit", "thehobbit" )
		);
	}

	private void makeMoviesForNumericIdTermQuery() {
		// just some random data:
		helper.add(
				new Movie( 3, "Cashback" ),
				new Movie( 4, "Garden state" )
		);
	}

	private void makeBooksForNumRangeQuery() {
		// just some random data:
		helper.add(
				new Book( String.valueOf( 5 ), 1, 1L, 1F, 1D, "Lord of The Rings", "lordoftherings" ),
				new Book( String.valueOf( 6 ), 2, 2L, 2F, 2D, "The Hobbit", "thehobbit" )
		);
	}

	@Indexed(index = "books")
	private static class Book {

		@DocumentId
		String id;
		@Field(store = Store.YES, index = Index.YES)
		Integer intField;
		@Field(store = Store.YES, index = Index.YES)
		Long longField;
		@Field(store = Store.YES, index = Index.YES)
		Float floatField;
		@Field(store = Store.YES, index = Index.YES)
		Double doubleField;
		@Field
		String title;
		@Field
		String url;

		public Book(String id, String title, String url) {
			this.id = id;
			this.title = title;
			this.url = url;
		}

		public Book(String id, Integer intField, Long longField, Float floatField, Double doubleField, String title, String url) {
			this.id = id;
			this.intField = intField;
			this.longField = longField;
			this.floatField = floatField;
			this.doubleField = doubleField;
			this.title = title;
			this.url = url;
		}

	}

	@Indexed(index = "movies")
	private static class Movie {
		@DocumentId
		Integer id;

		@Field
		String title;

		public Movie(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}

}
