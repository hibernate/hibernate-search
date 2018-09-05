/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.assertFieldSelectorDisabled;
import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.assertFieldSelectorEnabled;
import static org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider.resetFieldSelector;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests aspects of projection that are specific to the Lucene Backend.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // This test is specific to the Lucene backend
public class LuceneProjectionQueryTest extends SearchTestBase {

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.directory_provider", "local-heap" );
		cfg.put( "hibernate.search.default." + Environment.READER_STRATEGY, FieldSelectorLeakingReaderProvider.class.getName() );
	}

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

		s.persist( new CalendarDay().setDayFromItalianString( "01/04/2011" ) );
		s.persist( new CalendarDay().setDayFromItalianString( "02/04/2011" ) );

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
	public void testProjectionOfThisFieldSelector() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.THIS );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertFalse( result.isEmpty() );
		assertFieldSelectorEnabled( "id" );

		tx.commit();
	}

	@Test
	public void testClassProjectionFieldSelector() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.OBJECT_CLASS );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertFalse( result.isEmpty() );
		assertFieldSelectorEnabled( ); // empty!

		tx.commit();
	}

	@Test
	public void testStoredFieldProjectionFieldSelector() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept" );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertFalse( result.isEmpty() );
		assertFieldSelectorEnabled( "lastname", "dept", "id" );

		tx.commit();
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
		assertEquals( "DOCUMENT size incorrect", 5, ( (Document) projection[0] ).getFields().size() );

		tx.commit();
	}

	@Test
	public void testLuceneDocumentProjectionFieldSelector() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.DOCUMENT );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertFalse( result.isEmpty() );
		assertFieldSelectorDisabled(); //because of DOCUMENT being projected

		tx.commit();
	}

	@Test
	public void testLuceneDocumentIdProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.DOCUMENT_ID );

		List<?> result = hibQuery.list();
		assertNotNull( result );
		Object[] projection = (Object[]) result.get( 0 );
		assertTrue( "DOCUMENT_ID incorrect", projection[0] instanceof Integer );

		tx.commit();
	}

	@Test
	public void testLuceneDocumentIdProjectionFieldSelector() throws Exception {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.DOCUMENT_ID );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertFalse( result.isEmpty() );
		assertFieldSelectorDisabled(); //because of only DOCUMENT_ID being projected

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
		assertEquals( "DOCUMENT size incorrect", 5, ( (Document) projection[1] ).getFields().size() );

		tx.commit();
	}

	@Test
	public void testProjectionUnmappedFieldValues() throws ParseException, IOException {
		FullTextSession s = Search.getFullTextSession( getSession() );
		Transaction tx = s.beginTransaction();

		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( new MatchAllDocsQuery(), CalendarDay.class );
		hibQuery.setProjection( "day.year" );

		resetFieldSelector();
		List<?> result = hibQuery.list();
		assertFieldSelectorEnabled( ); //empty: can't use one as the bridge we use mandates optimisations to be disabled
		assertNotNull( result );
		assertFalse( result.isEmpty() );

		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
				CalendarDay.class
		};
	}

}
