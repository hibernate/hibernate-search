/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.stat.Statistics;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class LuceneQueryTest extends SearchTestBase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		indexTestData();
	}

	@Test
	public void testList() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( 0, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with explicit class filter", 2, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with one class filter", 1, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no class filter", 2, result.size() );
		for ( Object element : result ) {
			assertTrue( Hibernate.isInitialized( element ) );
			fullTextSession.delete( element );
		}
		fullTextSession.flush();
		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with delete objects", 0, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testResultSize() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		Statistics stats = fullTextSession.getSessionFactory().getStatistics();
		stats.clear();
		boolean enabled = stats.isStatisticsEnabled();
		if ( !enabled ) {
			stats.setStatisticsEnabled( true );
		}
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		assertEquals( "Exection of getResultSize without actual results", 2, hibQuery.getResultSize() );
		assertEquals( "No entity should be loaded", 0, stats.getEntityLoadCount() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "2 entities should be loaded", 2, stats.getEntityLoadCount() );
		if ( !enabled ) {
			stats.setStatisticsEnabled( false );
		}

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testResultSizeWithOffset() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 1 );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "first result no max result", 1, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testMaxResultLessThanTotalNumberOfHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 1 );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "max result set", 1, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testMaxResultMoreThanTotalNumberOfHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 3 );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "max result out of limit", 2, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testMaxResultWithOffset() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );

		hibQuery.setFirstResult( 2 );
		hibQuery.setMaxResults( 3 );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "first result out of limit", 0, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testScrollableResultSet() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		ScrollableResults result = hibQuery.scroll();
		assertNotNull( result );
		assertEquals( -1, result.getRowNumber() );
		assertEquals( false, result.next() );
		result.close();

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.scroll();
		assertEquals( -1, result.getRowNumber() );
		result.beforeFirst();
		assertEquals( true, result.next() );
		assertTrue( result.isFirst() );
		assertTrue( result.scroll( 1 ) );
		assertTrue( result.isLast() );
		assertFalse( result.scroll( 1 ) );

		tx.commit();
		fullTextSession.close();
	}

	// Technically this is checked by other tests but let's do it anyway. J.G.

	@Test
	public void testDefaultFetchSize() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		Object[] projection = (Object[]) projections.get();
		assertNull( projection );

		projections.next();
		assertTrue( projections.isFirst() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchSizeLargerThanHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 6 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchSizeDefaultMax() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );
		results.scroll( 2 );
		result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1003, result[0] );
		// check cache addition
		results.next();
		result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1004, result[0] );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchSizeNonDefaultMax() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );
		hibQuery.setMaxResults( 4 );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );

		results.next();
		result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1002, result[0] );

		results.scroll( 2 );
		result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1004, result[0] );

		results.next();
		result = (Object[]) results.get();
		assertNull( result );

		results.close();

		results = hibQuery.scroll();

		results.beforeFirst();
		results.next();
		result = (Object[]) results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );

		// And test a bad forward scroll.
		results.scroll( 10 );
		result = (Object[]) results.get();
		assertNull( result );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchSizeNonDefaultMaxNoHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );
		hibQuery.setMaxResults( 3 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		Object[] result = (Object[]) results.get();
		assertNull( "non-null entity infos returned", result );

		tx.commit();
		fullTextSession.close();
	}

	/**
	 * Test for HSEARCH-604. Tests that max result 0 does not throw an exception.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testMaxResultZero() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:foo" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 0 );

		List result = hibQuery.list();
		assertTrue( "We should get the empty result list", result.isEmpty() );

		hibQuery.setFirstResult( 1 );
		hibQuery.setMaxResults( 0 );

		result = hibQuery.list();
		assertTrue( "We should get the empty result list", result.isEmpty() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testCurrent() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );
		hibQuery.setProjection( "id", "lastname", "dept" );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		assertTrue( "beforeFirst() pointer incorrect", results.isFirst() );

		results.last();
		assertTrue( "last() pointer incorrect", results.isLast() );

		results.afterLast();
		assertEquals( "afterLast() pointer incorrect", -1, results.getRowNumber() );

		// Let's test a REAL screwup.
		hibQuery.setMaxResults( 4 );

		results = hibQuery.scroll();
		results.scroll( 4 );
		Object[] result = (Object[]) results.get();
		assertEquals( 1004, result[0] );

		results.last();
		result = (Object[]) results.get();
		assertEquals( 1004, result[0] );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testScrollFirstResult() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );
		hibQuery.setProjection( "id", "lastname", "dept" );

		hibQuery.setFirstResult( 3 );
		hibQuery.setMaxResults( 1 );

		assertThatThrownBy( () -> hibQuery.scroll() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use firstResult > 0 with scrolls" );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testNoGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no explicit entity graph", 1, result.size() );
		Book book = (Book) result.get( 0 );
		assertFalse( "Association should not be initialized", Hibernate.isInitialized( book.getAuthors() ) );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testLoadGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.applyLoadGraph( graph )
				.list();
		assertNotNull( result );
		assertEquals( "Query with explicit entity graph", 1, result.size() );
		Book book = (Book) result.get( 0 );
		assertTrue( "Association should be initialized", Hibernate.isInitialized( book.getAuthors() ) );
		assertEquals( 1, book.getAuthors().size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.applyFetchGraph( graph )
				.list();
		assertNotNull( result );
		assertEquals( "Query with explicit entity graph", 1, result.size() );
		Book book = (Book) result.get( 0 );
		assertTrue( "Association should be initialized", Hibernate.isInitialized( book.getAuthors() ) );
		assertEquals( 1, book.getAuthors().size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testLoadGraphHint() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.setHint( "jakarta.persistence.loadgraph", graph )
				.list();
		assertNotNull( result );
		assertEquals( "Query with explicit entity graph", 1, result.size() );
		Book book = (Book) result.get( 0 );
		assertTrue( "Association should be initialized", Hibernate.isInitialized( book.getAuthors() ) );
		assertEquals( 1, book.getAuthors().size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testFetchGraphHint() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.setHint( "jakarta.persistence.fetchgraph", graph )
				.list();
		assertNotNull( result );
		assertEquals( "Query with explicit entity graph", 1, result.size() );
		Book book = (Book) result.get( 0 );
		assertTrue( "Association should be initialized", Hibernate.isInitialized( book.getAuthors() ) );
		assertEquals( 1, book.getAuthors().size() );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testScrollEmptyHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		Object[] projection = (Object[]) projections.get();
		assertNull( projection );

		hibQuery = fullTextSession.createFullTextQuery( query, Employee.class ).setMaxResults( 20 );

		projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		projection = (Object[]) projections.get();
		assertNull( projection );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testListEmptyHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		List result = hibQuery.list();
		assertEquals( 0, result.size() );

		hibQuery = fullTextSession.createFullTextQuery( query, Employee.class ).setFirstResult( 10 ).setMaxResults( 20 );
		result = hibQuery.list();
		assertEquals( 0, result.size() );

		tx.commit();
		fullTextSession.close();
	}

	private void indexTestData() {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		Clock clock = new Clock( 1, "Seiko" );
		fullTextSession.save( clock );
		clock = new Clock( 2, "Festina" );
		fullTextSession.save( clock );

		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		fullTextSession.save( book );

		Author emmanuel = new Author();
		emmanuel.setName( "Emmanuel" );
		fullTextSession.save( emmanuel );
		book.getAuthors().add( emmanuel );

		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		fullTextSession.save( book );

		fullTextSession.save( new Employee( 1000, "Griffin", "ITech" ) );
		fullTextSession.save( new Employee( 1001, "Jackson", "Accounting" ) );
		fullTextSession.save( new Employee( 1002, "Jimenez", "ITech" ) );
		fullTextSession.save( new Employee( 1003, "Stejskal", "ITech" ) );
		fullTextSession.save( new Employee( 1004, "Whetbrook", "ITech" ) );

		tx.commit();
		fullTextSession.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Clock.class,
				Author.class,
				Employee.class
		};
	}
}
