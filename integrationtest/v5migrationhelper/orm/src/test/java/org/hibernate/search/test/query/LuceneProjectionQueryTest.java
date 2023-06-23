/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Tests aspects of projection that are specific to the Lucene Backend.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // This test is specific to the Lucene backend
public class LuceneProjectionQueryTest extends SearchTestBase {

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Employee e1 = new Employee( 1000, "Griffin", "ITech" );
		s.save( e1 );
		Employee e2 = new Employee( 1001, "Jackson", "Accounting" );
		e2.setHireDate( new Date() );
		s.save( e2 );
		Employee e3 = new Employee( 1002, "Jimenez", "ITech" );
		s.save( e3 );
		Employee e4 = new Employee( 1003, "Stejskal", "ITech" );
		s.save( e4 );
		Employee e5 = new Employee( 1004, "Whetbrook", "ITech" );
		s.save( e5 );

		tx.commit();
		s.clear();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		Session s = getSession(); // Opened during setup
		try {
			Transaction tx = s.beginTransaction();
			for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
				s.delete( element );
			}
			tx.commit();
		}
		finally {
			s.close();
		}
		super.tearDown();
	}

	@Test
	public void testLuceneDocumentProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.DOCUMENT );

		List<?> result = hibQuery.list();
		assertNotNull( result );
		Object[] projection = (Object[]) result.get( 0 );
		assertTrue( "DOCUMENT incorrect", projection[0] instanceof Document );
		assertEquals( "DOCUMENT size incorrect", 4, ( (Document) projection[0] ).getFields().size() );

		tx.commit();
	}

	@Test
	public void testLuceneDocumentProjectionNonLoadedFieldOptimization() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.ID, FullTextQuery.DOCUMENT );

		List<?> result = hibQuery.list();
		assertNotNull( result );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		assertEquals( "id field name not projected", 1001, projection[0] );
		assertEquals(
				"Document fields should not be lazy on DOCUMENT projection",
				"Jackson", ( (Document) projection[1] ).getField( "lastname" ).stringValue()
		);
		assertEquals( "DOCUMENT size incorrect", 4, ( (Document) projection[1] ).getFields().size() );

		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class
		};
	}

}
