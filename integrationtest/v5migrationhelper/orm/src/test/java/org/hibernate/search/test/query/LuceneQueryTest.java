/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
class LuceneQueryTest extends SearchTestBase {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		indexTestData();
	}

	@Test
	void testList() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).isEmpty();

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with explicit class filter" ).hasSize( 2 );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with one class filter" ).hasSize( 1 );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no class filter" ).hasSize( 2 );
		for ( Object element : result ) {
			assertThat( Hibernate.isInitialized( element ) ).isTrue();
			fullTextSession.delete( element );
		}
		fullTextSession.flush();
		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with delete objects" ).isEmpty();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testResultSize() throws Exception {
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
		assertThat( hibQuery.getResultSize() ).as( "Exection of getResultSize without actual results" ).isEqualTo( 2 );
		assertThat( stats.getEntityLoadCount() ).as( "No entity should be loaded" ).isEqualTo( 0 );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( stats.getEntityLoadCount() ).as( "2 entities should be loaded" ).isEqualTo( 2 );
		if ( !enabled ) {
			stats.setStatisticsEnabled( false );
		}

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testResultSizeWithOffset() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 1 );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "first result no max result" ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testMaxResultLessThanTotalNumberOfHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 1 );
		List result = hibQuery.list();
		assertThat( result ).isNotNull()
				.hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testMaxResultMoreThanTotalNumberOfHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 3 );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "max result out of limit" ).hasSize( 2 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testMaxResultWithOffset() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );

		hibQuery.setFirstResult( 2 );
		hibQuery.setMaxResults( 3 );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "first result out of limit" ).isEmpty();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testScrollableResultSet() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		ScrollableResults result = hibQuery.scroll();
		assertThat( result ).isNotNull();
		assertThat( result.getRowNumber() ).isEqualTo( -1 );
		assertThat( result.next() ).isFalse();
		result.close();

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = fullTextSession.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.scroll();
		assertThat( result.getRowNumber() ).isEqualTo( -1 );
		result.beforeFirst();
		assertThat( result.next() ).isTrue();
		assertThat( result.isFirst() ).isTrue();
		assertThat( result.scroll( 1 ) ).isTrue();
		assertThat( result.isLast() ).isTrue();
		assertThat( result.scroll( 1 ) ).isFalse();

		tx.commit();
		fullTextSession.close();
	}

	// Technically this is checked by other tests but let's do it anyway. J.G.

	@Test
	void testDefaultFetchSize() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		Object[] projection = (Object[]) projections.get();
		assertThat( projection ).isNull();

		projections.next();
		assertThat( projections.isFirst() ).isTrue();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchSizeLargerThanHits() throws Exception {
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
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1000 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchSizeDefaultMax() throws Exception {
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
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1000 );
		results.scroll( 2 );
		result = (Object[]) results.get();
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1003 );
		// check cache addition
		results.next();
		result = (Object[]) results.get();
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1004 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchSizeNonDefaultMax() throws Exception {
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
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1000 );

		results.next();
		result = (Object[]) results.get();
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1002 );

		results.scroll( 2 );
		result = (Object[]) results.get();
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1004 );

		results.next();
		result = (Object[]) results.get();
		assertThat( result ).isNull();

		results.close();

		results = hibQuery.scroll();

		results.beforeFirst();
		results.next();
		result = (Object[]) results.get();
		assertThat( result[0] ).as( "incorrect entityInfo returned" ).isEqualTo( 1000 );

		// And test a bad forward scroll.
		results.scroll( 10 );
		result = (Object[]) results.get();
		assertThat( result ).isNull();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchSizeNonDefaultMaxNoHits() throws Exception {
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
		assertThat( result ).as( "non-null entity infos returned" ).isNull();

		tx.commit();
		fullTextSession.close();
	}

	/**
	 * Test for HSEARCH-604. Tests that max result 0 does not throw an exception.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	void testMaxResultZero() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:foo" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 0 );

		List result = hibQuery.list();
		assertThat( result ).as( "We should get the empty result list" ).isEmpty();

		hibQuery.setFirstResult( 1 );
		hibQuery.setMaxResults( 0 );

		result = hibQuery.list();
		assertThat( result ).as( "We should get the empty result list" ).isEmpty();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testCurrent() throws Exception {
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
		assertThat( results.isFirst() ).as( "beforeFirst() pointer incorrect" ).isTrue();

		results.last();
		assertThat( results.isLast() ).as( "last() pointer incorrect" ).isTrue();

		results.afterLast();
		assertThat( results.getRowNumber() ).as( "afterLast() pointer incorrect" ).isEqualTo( -1 );

		// Let's test a REAL screwup.
		hibQuery.setMaxResults( 4 );

		results = hibQuery.scroll();
		results.scroll( 4 );
		Object[] result = (Object[]) results.get();
		assertThat( result[0] ).isEqualTo( 1004 );

		results.last();
		result = (Object[]) results.get();
		assertThat( result[0] ).isEqualTo( 1004 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testScrollFirstResult() throws Exception {
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
	void testNoGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );
		org.hibernate.query.Query hibQuery = fullTextSession.createFullTextQuery( query, Book.class );
		List result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit entity graph" ).hasSize( 1 );
		Book book = (Book) result.get( 0 );
		assertThat( Hibernate.isInitialized( book.getAuthors() ) ).as( "Association should not be initialized" ).isFalse();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testLoadGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.applyLoadGraph( graph )
				.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit entity graph" ).hasSize( 1 );
		Book book = (Book) result.get( 0 );
		assertThat( Hibernate.isInitialized( book.getAuthors() ) ).as( "Association should be initialized" ).isTrue();
		assertThat( book.getAuthors() ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchGraph() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.applyFetchGraph( graph )
				.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit entity graph" ).hasSize( 1 );
		Book book = (Book) result.get( 0 );
		assertThat( Hibernate.isInitialized( book.getAuthors() ) ).as( "Association should be initialized" ).isTrue();
		assertThat( book.getAuthors() ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testLoadGraphHint() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.setHint( "jakarta.persistence.loadgraph", graph )
				.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit entity graph" ).hasSize( 1 );
		Book book = (Book) result.get( 0 );
		assertThat( Hibernate.isInitialized( book.getAuthors() ) ).as( "Association should be initialized" ).isTrue();
		assertThat( book.getAuthors() ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testFetchGraphHint() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );

		RootGraph<Book> graph = fullTextSession.createEntityGraph( Book.class );
		graph.addAttributeNodes( "authors" );

		List result = fullTextSession.createFullTextQuery( query, Book.class )
				.setHint( "jakarta.persistence.fetchgraph", graph )
				.list();
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit entity graph" ).hasSize( 1 );
		Book book = (Book) result.get( 0 );
		assertThat( Hibernate.isInitialized( book.getAuthors() ) ).as( "Association should be initialized" ).isTrue();
		assertThat( book.getAuthors() ).hasSize( 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testScrollEmptyHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		Object[] projection = (Object[]) projections.get();
		assertThat( projection ).isNull();

		hibQuery = fullTextSession.createFullTextQuery( query, Employee.class ).setMaxResults( 20 );

		projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		projection = (Object[]) projections.get();
		assertThat( projection ).isNull();

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testListEmptyHits() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = fullTextSession.createFullTextQuery( query, Employee.class );
		List result = hibQuery.list();
		assertThat( result ).isEmpty();

		hibQuery = fullTextSession.createFullTextQuery( query, Employee.class ).setFirstResult( 10 ).setMaxResults( 20 );
		result = hibQuery.list();
		assertThat( result ).isEmpty();

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
