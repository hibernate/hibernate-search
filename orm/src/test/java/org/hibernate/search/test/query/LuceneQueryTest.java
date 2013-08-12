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

import java.util.Iterator;
import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import org.hibernate.FetchMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.impl.HibernateHelper;
import org.hibernate.stat.Statistics;

/**
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class LuceneQueryTest extends SearchTestCase {

	public void testList() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		clock = new Clock( 2, "Festina" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		s.save( book );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( 0, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with explicit class filter", 2, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with one class filter", 1, result.size() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no class filter", 2, result.size() );
		for ( Object element : result ) {
			assertTrue( HibernateHelper.isInitialized( element ) );
			s.delete( element );
		}
		s.flush();
		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with delete objects", 0, result.size() );

		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testResultSize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		clock = new Clock( 2, "Festina" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		s.save( book );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		Statistics stats = s.getSessionFactory().getStatistics();
		stats.clear();
		boolean enabled = stats.isStatisticsEnabled();
		if ( !enabled ) {
			stats.setStatisticsEnabled( true );
		}
		FullTextQuery hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		assertEquals( "Exection of getResultSize without actual results", 2, hibQuery.getResultSize() );
		assertEquals( "No entity should be loaded", 0, stats.getEntityLoadCount() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "2 entities should be loaded", 2, stats.getEntityLoadCount() );
		if ( !enabled ) {
			stats.setStatisticsEnabled( false );
		}
		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testFirstMax() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		clock = new Clock( 2, "Festina" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		s.save( book );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina Or brand:Seiko" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setFirstResult( 1 );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "first result no max result", 1, result.size() );

		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 1 );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "max result set", 1, result.size() );

		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 3 );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "max result out of limit", 2, result.size() );

		hibQuery.setFirstResult( 2 );
		hibQuery.setMaxResults( 3 );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "first result out of limit", 0, result.size() );

		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testIterator() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		clock = new Clock( 2, "Festina" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		s.save( book );
		tx.commit();//post commit events for lucene
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		Iterator result = hibQuery.iterate();
		assertNotNull( result );
		assertFalse( result.hasNext() );

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.iterate();
		assertNotNull( result );
		int index = 0;
		while ( result.hasNext() ) {
			index++;
			s.delete( result.next() );
		}
		assertEquals( 2, index );

		s.flush();

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.iterate();
		assertNotNull( result );
		assertFalse( result.hasNext() );

		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testScrollableResultSet() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		clock = new Clock( 2, "Festina" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		book = new Book( 2, "La gloire de mon père", "Les deboires de mon père en vélo" );
		s.save( book );
		tx.commit();//post commit events for lucene
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:noword" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		ScrollableResults result = hibQuery.scroll();
		assertNotNull( result );
		assertEquals( -1, result.getRowNumber() );
		assertEquals( false, result.next() );
		result.close();

		query = parser.parse( "summary:Festina Or brand:Seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		result = hibQuery.scroll();
		assertEquals( -1, result.getRowNumber() );
		result.beforeFirst();
		assertEquals( true, result.next() );
		assertTrue( result.isFirst() );
		assertTrue( result.scroll( 1 ) );
		assertTrue( result.isLast() );
		assertFalse( result.scroll( 1 ) );
		result.beforeFirst();
		while ( result.next() ) {
			s.delete( result.get()[0] );
		}
		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	// Technically this is checked by other tests but let's do it anyway. J.G.

	public void testDefaultFetchSize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		Object[] projection = projections.get();
		assertNull( projection );

		projections.next();
		assertTrue( projections.isFirst() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testFetchSizeLargerThanHits() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 6 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testFetchSizeDefaultFirstAndMax() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.STRING ) ) );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = results.get();
		assertEquals( "incorrect entityInfo returned", 1000, result[0] );
		results.scroll( 2 );
		result = results.get();
		assertEquals( "incorrect entityInfo returned", 1003, result[0] );
		// check cache addition
		results.next();
		result = results.get();
		assertEquals( "incorrect entityInfo returned", 1004, result[0] );

		results.scroll( -2 );
		result = results.get();
		assertEquals( "incorrect entityInfo returned", 1002, result[0] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testFetchSizeNonDefaultFirstAndMax() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );
		hibQuery.setFirstResult( 1 );
		hibQuery.setMaxResults( 3 );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.STRING ) ) );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		Object[] result = results.get();
		assertEquals( "incorrect entityInfo returned", 1002, result[0] );

		results.scroll( 2 );
		result = results.get();
		assertEquals( "incorrect entityInfo returned", 1004, result[0] );

		results.next();
		result = results.get();
		assertNull( result );

		results.scroll( -8 );
		result = results.get();
		assertNull( result );

		// And test a bad forward scroll.
		results.scroll( 10 );
		result = results.get();
		assertNull( result );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testFetchSizeNonDefaultFirstAndMaxNoHits() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );
		hibQuery.setFetchSize( 3 );
		hibQuery.setFirstResult( 1 );
		hibQuery.setMaxResults( 3 );

		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		Object[] result = results.get();
		assertNull( "non-null entity infos returned", result );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	/**
	 * Test for HSEARCH-604. Tests that max result 0 does not throw an exception.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testMaxResultZero() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:foo" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setFirstResult( 0 );
		hibQuery.setMaxResults( 0 );

		List result = hibQuery.list();
		assertTrue( "We should get the empty result list", result.isEmpty() );

		hibQuery.setFirstResult( 1 );
		hibQuery.setMaxResults( 0 );

		result = hibQuery.list();
		assertTrue( "We should get the empty result list", result.isEmpty() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testCurrent() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );


		ScrollableResults results = hibQuery.scroll();
		results.beforeFirst();
		results.next();
		assertTrue( "beforeFirst() pointer incorrect", results.isFirst() );

		results.afterLast();
		results.previous();
		assertTrue( "afterLast() pointer incorrect", results.isLast() );

		// Let's see if a bad reverse scroll screws things up
		results.scroll( -8 );
		results.next();
		assertTrue( "large negative scroll() pointer incorrect", results.isFirst() );

		// And test a bad forward scroll.
		results.scroll( 10 );
		results.previous();
		assertTrue( "large positive scroll() pointer incorrect", results.isLast() );

		// Finally, let's test a REAL screwup.
		hibQuery.setFirstResult( 3 );
		hibQuery.setMaxResults( 1 );

		results = hibQuery.scroll();
		results.first();
		Object[] result = results.get();
		assertEquals( 1004, result[0] );

		results.last();
		result = results.get();
		assertEquals( 1004, result[0] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testMultipleEntityPerIndex() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Clock clock = new Clock( 1, "Seiko" );
		s.save( clock );
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		AlternateBook alternateBook = new AlternateBook(
				1, "La chute de la petite reine a travers les yeux de Festina"
		);
		s.save( alternateBook );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with explicit class filter", 1, result.size() );

		query = parser.parse( "summary:Festina" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		Iterator it = hibQuery.iterate();
		assertTrue( it.hasNext() );
		assertNotNull( it.next() );
		assertFalse( it.hasNext() );

		query = parser.parse( "summary:Festina" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		ScrollableResults sr = hibQuery.scroll();
		assertTrue( sr.first() );
		assertNotNull( sr.get() );
		assertFalse( sr.next() );
		sr.close();

		query = parser.parse( "summary:Festina OR brand:seiko" );
		hibQuery = s.createFullTextQuery( query, Clock.class, Book.class );
		hibQuery.setMaxResults( 2 );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with explicit class filter and limit", 2, result.size() );

		query = parser.parse( "summary:Festina" );
		hibQuery = s.createFullTextQuery( query );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no class filter", 2, result.size() );
		for ( Object element : result ) {
			assertTrue( HibernateHelper.isInitialized( element ) );
			s.delete( element );
		}
		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testCriteria() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		Author emmanuel = new Author();
		emmanuel.setName( "Emmanuel" );
		s.save( emmanuel );
		book.getAuthors().add( emmanuel );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );
		org.hibernate.Query hibQuery = s.createFullTextQuery( query, Book.class );
		List result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no explicit criteria", 1, result.size() );
		book = (Book) result.get( 0 );
		assertFalse( "Association should not be inintialized", HibernateHelper.isInitialized( book.getAuthors() ) );

		result = s.createFullTextQuery( query ).setCriteriaQuery(
				s.createCriteria( Book.class ).setFetchMode( "authors", FetchMode.JOIN )
		).list();
		assertNotNull( result );
		assertEquals( "Query with explicit criteria", 1, result.size() );
		book = (Book) result.get( 0 );
		assertTrue( "Association should be inintialized", HibernateHelper.isInitialized( book.getAuthors() ) );
		assertEquals( 1, book.getAuthors().size() );

		//cleanup
		Author author = book.getAuthors().iterator().next();
		book.getAuthors().remove( author );

		for ( Object element : s.createQuery( "from java.lang.Object" ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testScrollEmptyHits() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );

		ScrollableResults projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		Object[] projection = projections.get();
		assertNull( projection );

		hibQuery = s.createFullTextQuery( query, Employee.class ).setFirstResult( 10 ).setMaxResults( 20 );

		projections = hibQuery.scroll();
		projections.beforeFirst();
		projections.next();
		projection = projections.get();
		assertNull( projection );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testListEmptyHits() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		List result = hibQuery.list();
		assertEquals( 0, result.size() );

		hibQuery = s.createFullTextQuery( query, Employee.class ).setFirstResult( 10 ).setMaxResults( 20 );
		result = hibQuery.list();
		assertEquals( 0, result.size() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	public void testIterateEmptyHits() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:XXX" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		Iterator iter = hibQuery.iterate();
		assertFalse( iter.hasNext() );

		hibQuery = s.createFullTextQuery( query, Employee.class ).setFirstResult( 10 ).setMaxResults( 20 );
		iter = hibQuery.iterate();
		assertFalse( iter.hasNext() );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	private void prepEmployeeIndex(FullTextSession s) {
		Transaction tx = s.beginTransaction();
		s.save( new Employee( 1000, "Griffin", "ITech" ) );
		s.save( new Employee( 1001, "Jackson", "Accounting" ) );
		s.save( new Employee( 1002, "Jimenez", "ITech" ) );
		s.save( new Employee( 1003, "Stejskal", "ITech" ) );
		s.save( new Employee( 1004, "Whetbrook", "ITech" ) );
		tx.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				AlternateBook.class,
				Clock.class,
				Author.class,
				Employee.class
		};
	}
}
