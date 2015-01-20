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
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.DeleteByQuerySupport;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.NumRangeQuery;
import org.hibernate.search.backend.SingularTermQuery;
import org.hibernate.search.backend.NumRangeQuery.Type;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;

public class DeleteByQueryTest {

	@Rule
	public SearchFactoryHolder factoryHolder = new SearchFactoryHolder( Book.class );

	@Test
	public void testStringSerialization() {
		for ( DeletionQuery q : this.buildQueries() ) {
			this.testSerializationForQuery( q );
		}
	}

	private List<DeletionQuery> buildQueries() {
		List<DeletionQuery> l = new ArrayList<>();

		l.add( new SingularTermQuery( "id", "123" ) );
		l.addAll( this.buildRangeQueries() );

		return l;
	}

	private List<NumRangeQuery> buildRangeQueries() {
		List<NumRangeQuery> l = new ArrayList<>();

		{
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, false, false ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, true, false ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, false, true ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, true, true ) );

			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, false, false, 8 ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, true, false, 8 ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, false, true, 8 ) );
			l.add( new NumRangeQuery( "intField", Type.INT, 1, 2, true, true, 8 ) );

			l.add( new NumRangeQuery( "intField", 1, 2, false, false ) );
			l.add( new NumRangeQuery( "intField", 1, 2, true, false ) );
			l.add( new NumRangeQuery( "intField", 1, 2, false, true ) );
			l.add( new NumRangeQuery( "intField", 1, 2, true, true ) );

			l.add( new NumRangeQuery( "intField", 1, 2, false, false, 8 ) );
			l.add( new NumRangeQuery( "intField", 1, 2, true, false, 8 ) );
			l.add( new NumRangeQuery( "intField", 1, 2, false, true, 8 ) );
			l.add( new NumRangeQuery( "intField", 1, 2, true, true, 8 ) );
		}

		{
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, false, false ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, true, false ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, false, true ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, true, true ) );

			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, false, false, 8 ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, true, false, 8 ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, false, true, 8 ) );
			l.add( new NumRangeQuery( "longField", Type.LONG, 1L, 2L, true, true, 8 ) );

			l.add( new NumRangeQuery( "longField", 1L, 2L, false, false ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, true, false ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, false, true ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, true, true ) );

			l.add( new NumRangeQuery( "longField", 1L, 2L, false, false, 8 ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, true, false, 8 ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, false, true, 8 ) );
			l.add( new NumRangeQuery( "longField", 1L, 2L, true, true, 8 ) );
		}

		{
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, false, false ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, true, false ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, false, true ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, true, true ) );
			
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, false, false, 8 ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, true, false, 8 ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, false, true, 8 ) );
			l.add( new NumRangeQuery( "floatField", Type.FLOAT, 1F, 2F, true, true, 8 ) );
			
			l.add( new NumRangeQuery( "floatField", 1F, 2F, false, false ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, true, false ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, false, true ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, true, true ) );
			
			l.add( new NumRangeQuery( "floatField", 1F, 2F, false, false, 8 ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, true, false, 8 ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, false, true, 8 ) );
			l.add( new NumRangeQuery( "floatField", 1F, 2F, true, true, 8 ) );
		}

		{
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, false, false ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, true, false ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, false, true ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, true, true ) );
			
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, false, false, 8 ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, true, false, 8 ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, false, true, 8 ) );
			l.add( new NumRangeQuery( "doubleField", Type.DOUBLE, 1D, 2D, true, true, 8 ) );
			
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, false, false ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, true, false ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, false, true ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, true, true ) );
			
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, false, false, 8 ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, true, false, 8 ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, false, true, 8 ) );
			l.add( new NumRangeQuery( "doubleField", 1D, 2D, true, true, 8 ) );
		}

		return l;
	}

	private List<Integer> expectedCount() {
		List<Integer> l = new ArrayList<>();

		for ( int i = 0; i < 4; ++i ) {
			l.add( 2 );
			l.add( 1 );
			l.add( 1 );
			l.add( 0 );

			l.add( 2 );
			l.add( 1 );
			l.add( 1 );
			l.add( 0 );

			l.add( 2 );
			l.add( 1 );
			l.add( 1 );
			l.add( 0 );

			l.add( 2 );
			l.add( 1 );
			l.add( 1 );
			l.add( 0 );
		}

		return l;
	}

	@Test
	public void testSingularTermQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();
		Worker worker = integrator.getWorker();

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			for ( Work work : this.makeBooksForSingularTermQuery() ) {
				worker.performWork( work, tc );
			}
			tc.end();
		}
		this.assertCount( 2, integrator );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, new SingularTermQuery( "id", String.valueOf( 5 ) ) ), tc );
			tc.end();
		}
		this.assertCount( 1, integrator );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, new SingularTermQuery( "id", String.valueOf( 6 ) ) ), tc );
			tc.end();
		}
		this.assertCount( 0, integrator );
		// this should stay empty now!
	}

	@Test
	public void testNumRangeQuery() {
		ExtendedSearchIntegrator integrator = this.factoryHolder.getSearchFactory();

		List<NumRangeQuery> rangeQueries = this.buildRangeQueries();
		List<Integer> expectedCount = this.expectedCount();
		assertEquals( expectedCount.size(), rangeQueries.size() );

		for ( int i = 0; i < rangeQueries.size(); ++i ) {
			try {
				this.testForQuery( integrator, rangeQueries.get( i ), expectedCount.get( i ) );
			}
			catch (Throwable e) {
				System.out.println( "ERROR " + rangeQueries.get( i ) + ". expected was: " + expectedCount.get( i ) );
				throw e;
			}
		}
	}

	private void testForQuery(ExtendedSearchIntegrator integrator, DeletionQuery query, int expectedCount) {
		Worker worker = integrator.getWorker();
		{
			TransactionContextForTest tc = new TransactionContextForTest();
			for ( Work work : this.makeBooksForNumRangeQuery() ) {
				worker.performWork( work, tc );
			}
			tc.end();
		}
		this.assertCount( 2, integrator );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new DeleteByQueryWork( Book.class, query ), tc );
			tc.end();
		}
		this.assertCount( expectedCount, integrator );

		{
			TransactionContextForTest tc = new TransactionContextForTest();
			worker.performWork( new Work( Book.class, null, WorkType.PURGE_ALL ), tc );
			tc.end();
		}
		this.assertCount( 0, integrator );
	}

	private void assertCount(int count, ExtendedSearchIntegrator integrator) {
		{
			Query query = integrator.buildQueryBuilder().forEntity( Book.class ).get().all().createQuery();
			HSQuery hsQuery = integrator.createHSQuery().luceneQuery( query );
			List<Class<?>> l = new ArrayList<>();
			l.add( Book.class );
			hsQuery.targetedEntities( l );
			assertEquals( count, hsQuery.queryResultSize() );
		}
	}

	private void testSerializationForQuery(DeletionQuery deletionQuery) {
		assertTrue( DeleteByQuerySupport.SUPPORTED_TYPES.containsKey( deletionQuery.getQueryKey() ) );
		assertEquals( deletionQuery.getClass(), DeleteByQuerySupport.SUPPORTED_TYPES.get( deletionQuery.getQueryKey() ) );
		String[] strRep = DeleteByQuerySupport.TO_STRING.get( deletionQuery.getQueryKey() ).toString( deletionQuery );
		DeletionQuery copy = DeleteByQuerySupport.FROM_STRING.get( deletionQuery.getQueryKey() ).fromString( strRep );
		assertEquals( deletionQuery, copy );
	}

	private List<Work> makeBooksForSingularTermQuery() {
		List<Work> list = new LinkedList<>();
		// just some random data:
		list.add( new Work( new Book( String.valueOf( 5 ), "Lord of The Rings" ), WorkType.ADD ) );
		list.add( new Work( new Book( String.valueOf( 6 ), "The Hobbit" ), WorkType.ADD ) );
		return list;
	}

	private List<Work> makeBooksForNumRangeQuery() {
		List<Work> list = new LinkedList<>();
		// just some random data:
		list.add( new Work( new Book( String.valueOf( 5 ), 1, 1L, 1F, 1D, "Lord of The Rings" ), WorkType.ADD ) );
		list.add( new Work( new Book( String.valueOf( 6 ), 2, 2L, 2F, 2D, "The Hobbit" ), WorkType.ADD ) );
		return list;
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

		public Book(String id, String title) {
			super();
			this.id = id;
			this.title = title;
		}

		public Book(String id, Integer intField, Long longField, Float floatField, Double doubleField, String title) {
			super();
			this.id = id;
			this.intField = intField;
			this.longField = longField;
			this.floatField = floatField;
			this.doubleField = doubleField;
			this.title = title;
		}

	}

}
