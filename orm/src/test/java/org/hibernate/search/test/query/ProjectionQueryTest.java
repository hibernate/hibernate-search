/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests several aspects of projection queries.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
public class ProjectionQueryTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-546")
	public void testProjectionOfThisAndEAGERFetching() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		Spouse spouse = new Spouse();
		spouse.setFirstName( "Christina" );
		s.save( spouse );
		Husband h = new Husband();
		h.setLastName( "Roberto" );
		h.setSpouse( spouse );
		s.save( h );
		tx.commit();

		s.clear();
		tx = s.beginTransaction();
		final QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Husband.class ).get();
		Query query = qb.keyword().onField( "lastName" ).matching( "Roberto" ).createQuery();
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Husband.class );
		hibQuery.setProjection( FullTextQuery.THIS );
		Criteria fetchingStrategy = s.createCriteria( Husband.class );
		fetchingStrategy.setFetchMode( "spouse", FetchMode.JOIN );
		hibQuery.setCriteriaQuery( fetchingStrategy );

		List<?> result = hibQuery.list();
		assertNotNull( result );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		final Husband husband = (Husband) projection[0];

		assertTrue( Hibernate.isInitialized( husband.getSpouse() ) );

		//cleanup
		for ( Object element : s.createQuery( "from " + Husband.class.getName() ).list() ) {
			s.delete( element );
		}
		for ( Object element : s.createQuery( "from " + Spouse.class.getName() ).list() ) {
			s.delete( element );
		}

		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-296")
	public void testClassProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.OBJECT_CLASS );

		List<?> result = hibQuery.list();
		assertNotNull( result );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		assertEquals( "Wrong projected class", Employee.class, projection[0] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}

		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionWithScroll() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ), SortField.FIELD_SCORE ) );
		hibQuery.setProjection(
				"id",
				"lastname",
				"dept",
				FullTextQuery.THIS,
				FullTextQuery.SCORE,
				FullTextQuery.ID
		);

		ScrollableResults projections = hibQuery.scroll();

		// There are a lot of methods to check in ScrollableResultsImpl
		// so, we'll use methods to check each projection as needed.

		projections.beforeFirst();
		projections.next();
		Object[] projection = projections.get();
		checkProjectionFirst( projection, s );
		assertTrue( projections.isFirst() );

		projections.last();
		projection = projections.get();
		checkProjectionLast( projection, s );
		assertTrue( projections.isLast() );

		projections.next();
		projection = projections.get();
		assertNull( projection );

		projections.previous();
		projection = projections.get();
		checkProjectionLast( projection, s );

		projections.first();
		projection = projections.get();
		checkProjectionFirst( projection, s );

		projections.scroll( 2 );
		projection = projections.get();
		checkProjection2( projection, s );

		projections.scroll( -5 );
		projection = projections.get();
		assertNull( projection );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testResultTransformToDelimString() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept", FullTextQuery.THIS, FullTextQuery.SCORE, FullTextQuery.ID );
		hibQuery.setResultTransformer( new ProjectionToDelimStringResultTransformer() );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ), SortField.FIELD_SCORE ) );

		@SuppressWarnings("unchecked")
		List<String> result = hibQuery.list();
		assertTrue( "incorrect transformation", result.get( 0 ).startsWith( "1000, Griffin, ITech" ) );
		assertTrue( "incorrect transformation", result.get( 1 ).startsWith( "1002, Jimenez, ITech" ) );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testResultTransformMap() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection(
				"id",
				"lastname",
				"dept",
				FullTextQuery.THIS,
				FullTextQuery.SCORE,
				FullTextQuery.ID
		);
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ), SortField.FIELD_SCORE ) );

		hibQuery.setResultTransformer( new ProjectionToMapResultTransformer() );

		List<?> transforms = hibQuery.list();
		Map<?, ?> map = (Map<?, ?>) transforms.get( 1 );
		assertEquals( "incorrect transformation", "ITech", map.get( "dept" ) );
		assertEquals( "incorrect transformation", 1002, map.get( "id" ) );
		assertEquals( "incorrect transformation", 1002, map.get( FullTextQuery.ID ) );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}


	@Test
	public void testTransformListIsCalled() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection(
				"id",
				"lastname",
				"dept",
				FullTextQuery.THIS,
				FullTextQuery.SCORE,
				FullTextQuery.ID
		);
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ), SortField.FIELD_SCORE ) );

		final CounterCallsProjectionToMapResultTransformer counters = new CounterCallsProjectionToMapResultTransformer();
		hibQuery.setResultTransformer( counters );

		hibQuery.list();
		assertEquals( counters.getTransformListCounter(), 1 );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	private void checkProjectionFirst(Object[] projection, Session s) {
		assertEquals( "id incorrect", 1000, projection[0] );
		assertEquals( "lastname incorrect", "Griffin", projection[1] );
		assertEquals( "dept incorrect", "ITech", projection[2] );
		assertEquals( "THIS incorrect", projection[3], s.get( Employee.class, (Serializable) projection[0] ) );
		assertTrue( "SCORE incorrect", projection[4] instanceof Float );
		assertEquals( "legacy ID incorrect", 1000, projection[5] );
	}

	private void checkProjectionLast(Object[] projection, Session s) {
		assertEquals( "id incorrect", 1004, projection[0] );
		assertEquals( "lastname incorrect", "Whetbrook", projection[1] );
		assertEquals( "dept incorrect", "ITech", projection[2] );
		assertEquals( "THIS incorrect", projection[3], s.get( Employee.class, (Serializable) projection[0] ) );
		assertTrue( "SCORE incorrect", projection[4] instanceof Float );
		assertEquals( "legacy ID incorrect", 1004, projection[5] );
	}

	private void checkProjection2(Object[] projection, Session s) {
		assertEquals( "id incorrect", 1003, projection[0] );
		assertEquals( "lastname incorrect", "Stejskal", projection[1] );
		assertEquals( "dept incorrect", "ITech", projection[2] );
		assertEquals( "THIS incorrect", projection[3], s.get( Employee.class, (Serializable) projection[0] ) );
		assertTrue( "SCORE incorrect", projection[4] instanceof Float );
		assertEquals( "legacy ID incorrect", 1003, projection[5] );
	}

	@Test
	public void testProjectionWithIterate() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection(
				"id", "lastname", "dept", FullTextQuery.THIS, FullTextQuery.SCORE,
				FullTextQuery.ID
		);

		int counter = 0;

		for ( Iterator<?> iter = hibQuery.iterate(); iter.hasNext(); ) {
			Object[] projection = (Object[]) iter.next();
			assertNotNull( projection );
			counter++;
			assertEquals( "dept incorrect", "ITech", projection[2] );
			assertEquals( "THIS incorrect", projection[3], s.get( Employee.class, (Serializable) projection[0] ) );
			assertTrue( "SCORE incorrect", projection[4] instanceof Float );
		}
		assertEquals( "incorrect number of results returned", 4, counter );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionWithList() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ), SortField.FIELD_SCORE ) );
		hibQuery.setProjection(
				"id", "lastname", "dept", FullTextQuery.THIS, FullTextQuery.SCORE,
				FullTextQuery.ID
		);

		List<?> result = hibQuery.list();
		assertNotNull( result );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		assertEquals( "id incorrect", 1001, projection[0] );
		assertEquals( "last name incorrect", "Jackson", projection[1] );
		assertEquals( "dept incorrect", "Accounting", projection[2] );
		assertEquals( "THIS incorrect", "Jackson", ( (Employee) projection[3] ).getLastname() );
		assertEquals( "THIS incorrect", projection[3], s.get( Employee.class, (Serializable) projection[0] ) );
		assertTrue( "SCORE incorrect", projection[4] instanceof Float );
		assertFalse( "SCORE should not be a NaN", Float.isNaN( (Float) projection[4] ) );
		assertEquals( "ID incorrect", 1001, projection[5] );

		// Change the projection order and null one
		hibQuery.setProjection(
				FullTextQuery.THIS, FullTextQuery.SCORE, null, FullTextQuery.ID,
				"id", "lastname", "dept", "hireDate"
		);

		result = hibQuery.list();
		assertNotNull( result );

		projection = (Object[]) result.get( 0 );
		assertNotNull( projection );

		assertEquals( "THIS incorrect", projection[0], s.get( Employee.class, (Serializable) projection[4] ) );
		assertTrue( "SCORE incorrect", projection[1] instanceof Float );
		assertNull( "BOOST not removed", projection[2] );
		assertEquals( "ID incorrect", 1001, projection[3] );
		assertEquals( "id incorrect", 1001, projection[4] );
		assertEquals( "last name incorrect", "Jackson", projection[5] );
		assertEquals( "dept incorrect", "Accounting", projection[6] );
		assertNotNull( "Date", projection[7] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionOnScoreWithoutRelevanceSort() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( new Sort( new SortField( "id", SortField.Type.STRING ) ) );
		hibQuery.setProjection(
				FullTextQuery.SCORE, FullTextQuery.ID
		);

		List<?> result = hibQuery.list();
		assertNotNull( result );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		assertTrue( "SCORE incorrect", projection[0] instanceof Float );
		assertFalse( "SCORE should not be a NaN", Float.isNaN( (Float) projection[0] ) );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testProjectionInNumericFields() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );

		Transaction tx = s.beginTransaction();
		FootballTeam chelsea = new FootballTeam( 1, "Chelsea", 0.5d, 4 );
		FootballTeam manUtd = new FootballTeam( 2, "Manchester United", 700.5d, 18 );
		FootballTeam liverpool = new FootballTeam( 3, "Liverpool", 502.4d, 18 );
		s.save( manUtd );
		s.save( liverpool );
		s.save( chelsea );

		tx.commit();

		s.clear();
		tx = s.beginTransaction();

		Query query = NumericFieldUtils.createNumericRangeQuery( "debtInMillions", 600d, 800d, true, true );

		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, FootballTeam.class );
		hibQuery.setProjection( "nrTitles", "name", "debtInMillions" );

		List<?> result = hibQuery.list();
		assertEquals( 1, result.size() );

		Object[] projection = (Object[]) result.get( 0 );
		assertNotNull( projection );
		assertTrue( "Numeric int Field not projected", projection[0] instanceof Integer );
		assertTrue( "String Field not projected", projection[1] instanceof String );
		assertTrue( "Numeric double Field not projected", projection[2] instanceof Double );

		assertEquals( 18, projection[0] );
		assertEquals( "Manchester United", projection[1] );
		assertEquals( 700.5d, projection[2] );

		//cleanup
		for ( Object element : s.createQuery( "from " + FootballTeam.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();

	}

	@Test
	public void testProjectionUnmappedFieldValues() throws ParseException, IOException {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( new CalendarDay().setDayFromItalianString( "01/04/2011" ) );
		s.persist( new CalendarDay().setDayFromItalianString( "02/04/2011" ) );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( new MatchAllDocsQuery(), CalendarDay.class );
		hibQuery.setProjection( "day_year" );

		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Wrong number of results", 2, result.size() );

		for ( Object resultLine : result ) {
			Object[] projection = (Object[]) resultLine;
			assertNotNull( projection );
			assertEquals( "Wrong projected result", "2011", projection[0] );
		}

		//cleanup
		for ( Object element : s.createQuery( "from " + CalendarDay.class.getName() ).list() ) {
			s.delete( element );
		}

		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2106")
	public void testUnexpectedProjectionConstant() {
		FullTextSession s = Search.getFullTextSession( openSession() );

		try {
			Query query = NumericFieldUtils.createNumericRangeQuery( "debtInMillions", 600d, 800d, true, true );

			FullTextQuery hibQuery = s.createFullTextQuery( query, FootballTeam.class );
			hibQuery.setProjection( "__HSearch_xyz" );

			fail();
		}
		catch (SearchException se) {
			assertTrue( "Unexpected message: " + se.getMessage(), se.getMessage().startsWith( "HSEARCH000317" ) );
		}
		finally {
			s.close();
		}
	}

	private void prepEmployeeIndex(FullTextSession s) {
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
	}

	@Test(expected = SearchException.class)
	@Category(ElasticsearchSupportInProgress.class) // HSEARCH-2423 Projecting an unstored field should raise an exception
	public void testProjectionOnUnstoredField() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		Book book2 = new Book( 2, "Sous les fleurs il n'y a rien", null );
		s.save( book2 );
		Author emmanuel = new Author();
		emmanuel.setName( "Emmanuel" );
		s.save( emmanuel );
		book.setMainAuthor( emmanuel );
		tx.commit();
		s.clear();

		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );
		Query query = parser.parse( "summary:Festina" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( "id", "body", "mainAuthor.name" );

		hibQuery.list();
	}

	@Test
	public void testProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		Book book = new Book(
				1,
				"La chute de la petite reine a travers les yeux de Festina",
				"La chute de la petite reine a travers les yeux de Festina, blahblah"
		);
		s.save( book );
		Book book2 = new Book( 2, "Sous les fleurs il n'y a rien", null );
		s.save( book2 );
		Author emmanuel = new Author();
		emmanuel.setName( "Emmanuel" );
		s.save( emmanuel );
		book.setMainAuthor( emmanuel );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "title", TestConstants.stopAnalyzer );

		Query query = parser.parse( "summary:Festina" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( "id", "summary", "mainAuthor.name" );

		List<?> result = hibQuery.list();
		assertNotNull( result );
		assertEquals( "Query with no explicit criteria", 1, result.size() );
		Object[] projection = (Object[]) result.get( 0 );
		assertEquals( "id", 1, projection[0] );
		assertEquals( "summary", "La chute de la petite reine a travers les yeux de Festina", projection[1] );
		assertEquals( "mainAuthor.name (embedded objects)", "Emmanuel", projection[2] );

		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection();
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( 1, result.size() );
		assertTrue( "Should not trigger projection", result.get( 0 ) instanceof Book );

		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( (String[]) null );
		result = hibQuery.list();
		assertNotNull( result );
		assertEquals( 1, result.size() );
		assertTrue( "Should not trigger projection", result.get( 0 ) instanceof Book );

		query = parser.parse( "summary:fleurs" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( "id", "summary", "mainAuthor.name" );
		result = hibQuery.list();
		hibQuery.setProjection( "id", "summary", "mainAuthor.name" );
		assertEquals( 1, result.size() );
		projection = (Object[]) result.get( 0 );
		assertEquals( "mainAuthor.name", null, projection[2] );

		//cleanup
		for ( Object element : s.createQuery( "from " + Book.class.getName() ).list() ) {
			s.delete( element );
		}
		for ( Object element : s.createQuery( "from " + Author.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class,
				Employee.class,
				Husband.class,
				Spouse.class,
				FootballTeam.class,
				CalendarDay.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.directory_provider", "local-heap" );
	}

}
