/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.Tags;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

/**
 * Tests several aspects of projection queries.
 *
 * @author Emmanuel Bernard
 * @author John Griffin
 * @author Hardy Ferentschik
 */
class ProjectionQueryTest extends SearchTestBase {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-546")
	void testProjectionOfThisAndEAGERFetching() {
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
		RootGraph<Husband> graph = s.createEntityGraph( Husband.class );
		graph.addAttributeNodes( "spouse" );
		hibQuery.applyLoadGraph( graph );

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();
		final Husband husband = (Husband) projection[0];

		assertThat( Hibernate.isInitialized( husband.getSpouse() ) ).isTrue();

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
	void testClassProjection() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		s.clear();
		Transaction tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		Query query = parser.parse( "dept:ITech" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( FullTextQuery.OBJECT_CLASS );

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();
		assertThat( projection[0] ).as( "Wrong projected class" ).isEqualTo( Employee.class );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}

		tx.commit();
		s.close();
	}

	@Test
	void testProjectionWithScroll() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).andByNative( SortField.FIELD_DOC ).createSort() );
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
		Object[] projection = (Object[]) projections.get();
		checkProjectionFirst( projection, s );
		assertThat( projections.isFirst() ).isTrue();

		projections.last();
		projection = (Object[]) projections.get();
		checkProjectionLast( projection, s );
		assertThat( projections.isLast() ).isTrue();

		projections.next();
		projection = (Object[]) projections.get();
		assertThat( projection ).isNull();

		projections.close();

		projections = hibQuery.scroll();

		projections.first();
		projection = (Object[]) projections.get();
		checkProjectionFirst( projection, s );

		projections.scroll( 2 );
		projection = (Object[]) projections.get();
		checkProjection2( projection, s );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	void testResultTransformToDelimString() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection( "id", "lastname", "dept", FullTextQuery.THIS, FullTextQuery.SCORE, FullTextQuery.ID );
		hibQuery.setResultTransformer( new ProjectionToDelimStringResultTransformer() );
		hibQuery.setSort( qb.sort().byField( "id" ).andByNative( SortField.FIELD_DOC ).createSort() );

		@SuppressWarnings("unchecked")
		List<String> result = hibQuery.list();
		assertThat( result.get( 0 ).startsWith( "1000, Griffin, ITech" ) ).as( "incorrect transformation" ).isTrue();
		assertThat( result.get( 1 ).startsWith( "1002, Jimenez, ITech" ) ).as( "incorrect transformation" ).isTrue();

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	void testResultTransformMap() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

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
		hibQuery.setSort( qb.sort().byField( "id" ).andByNative( SortField.FIELD_SCORE ).createSort() );

		hibQuery.setResultTransformer( new ProjectionToMapResultTransformer() );

		List<?> transforms = hibQuery.list();
		Map<?, ?> map = (Map<?, ?>) transforms.get( 1 );
		assertThat( map.get( "dept" ) ).as( "incorrect transformation" ).isEqualTo( "ITech" );
		assertThat( map.get( "id" ) ).as( "incorrect transformation" ).isEqualTo( 1002 );
		assertThat( map.get( FullTextQuery.ID ) ).as( "incorrect transformation" ).isEqualTo( 1002 );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}


	@Test
	void testTransformListIsCalled() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );

		Query query = parser.parse( "dept:ITech" );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setProjection(
				"id",
				"lastname",
				"dept",
				FullTextQuery.THIS,
				FullTextQuery.SCORE,
				FullTextQuery.ID
		);
		hibQuery.setSort( qb.sort().byField( "id" ).andByNative( SortField.FIELD_DOC ).createSort() );

		final CounterCallsProjectionToMapResultTransformer counters = new CounterCallsProjectionToMapResultTransformer();
		hibQuery.setResultTransformer( counters );

		hibQuery.list();
		assertThat( counters.getTransformListCounter() ).isEqualTo( 1 );

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	private void checkProjectionFirst(Object[] projection, Session s) {
		assertThat( projection[0] ).as( "id incorrect" ).isEqualTo( 1000 );
		assertThat( projection[1] ).as( "lastname incorrect" ).isEqualTo( "Griffin" );
		assertThat( projection[2] ).as( "dept incorrect" ).isEqualTo( "ITech" );
		assertThat( s.get( Employee.class, (Serializable) projection[0] ) ).as( "THIS incorrect" ).isEqualTo( projection[3] );
		assertThat( projection[4] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( projection[5] ).as( "legacy ID incorrect" ).isEqualTo( 1000 );
	}

	private void checkProjectionLast(Object[] projection, Session s) {
		assertThat( projection[0] ).as( "id incorrect" ).isEqualTo( 1004 );
		assertThat( projection[1] ).as( "lastname incorrect" ).isEqualTo( "Whetbrook" );
		assertThat( projection[2] ).as( "dept incorrect" ).isEqualTo( "ITech" );
		assertThat( s.get( Employee.class, (Serializable) projection[0] ) ).as( "THIS incorrect" ).isEqualTo( projection[3] );
		assertThat( projection[4] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( projection[5] ).as( "legacy ID incorrect" ).isEqualTo( 1004 );
	}

	private void checkProjection2(Object[] projection, Session s) {
		assertThat( projection[0] ).as( "id incorrect" ).isEqualTo( 1003 );
		assertThat( projection[1] ).as( "lastname incorrect" ).isEqualTo( "Stejskal" );
		assertThat( projection[2] ).as( "dept incorrect" ).isEqualTo( "ITech" );
		assertThat( s.get( Employee.class, (Serializable) projection[0] ) ).as( "THIS incorrect" ).isEqualTo( projection[3] );
		assertThat( projection[4] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( projection[5] ).as( "legacy ID incorrect" ).isEqualTo( 1003 );
	}

	@Test
	void testProjectionWithList() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).andByNative( SortField.FIELD_SCORE ).createSort() );
		hibQuery.setProjection(
				"id", "lastname", "dept", FullTextQuery.THIS, FullTextQuery.SCORE,
				FullTextQuery.ID
		);

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();
		assertThat( projection[0] ).as( "id incorrect" ).isEqualTo( 1001 );
		assertThat( projection[1] ).as( "last name incorrect" ).isEqualTo( "Jackson" );
		assertThat( projection[2] ).as( "dept incorrect" ).isEqualTo( "Accounting" );
		assertThat( ( (Employee) projection[3] ).getLastname() ).as( "THIS incorrect" ).isEqualTo( "Jackson" );
		assertThat( s.get( Employee.class, (Serializable) projection[0] ) ).as( "THIS incorrect" ).isEqualTo( projection[3] );
		assertThat( projection[4] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( Float.isNaN( (Float) projection[4] ) ).as( "SCORE should not be a NaN" ).isFalse();
		assertThat( projection[5] ).as( "ID incorrect" ).isEqualTo( 1001 );

		// Change the projection order and null one
		hibQuery.setProjection(
				FullTextQuery.THIS, FullTextQuery.SCORE, null, FullTextQuery.ID,
				"id", "lastname", "dept", "hireDate"
		);

		result = hibQuery.list();
		assertThat( result ).isNotNull();

		projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();

		assertThat( s.get( Employee.class, (Serializable) projection[4] ) ).as( "THIS incorrect" ).isEqualTo( projection[0] );
		assertThat( projection[1] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( projection[2] ).as( "BOOST not removed" ).isNull();
		assertThat( projection[3] ).as( "ID incorrect" ).isEqualTo( 1001 );
		assertThat( projection[4] ).as( "id incorrect" ).isEqualTo( 1001 );
		assertThat( projection[5] ).as( "last name incorrect" ).isEqualTo( "Jackson" );
		assertThat( projection[6] ).as( "dept incorrect" ).isEqualTo( "Accounting" );
		assertThat( projection[7] ).as( "Date" ).isNotNull();

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	void testProjectionOnScoreWithoutRelevanceSort() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		prepEmployeeIndex( s );

		Transaction tx;
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "dept", TestConstants.standardAnalyzer );
		QueryBuilder qb = s.getSearchFactory().buildQueryBuilder().forEntity( Employee.class ).get();

		Query query = parser.parse( "dept:Accounting" );
		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, Employee.class );
		hibQuery.setSort( qb.sort().byField( "id" ).createSort() );
		hibQuery.setProjection(
				FullTextQuery.SCORE, FullTextQuery.ID
		);

		List<?> result = hibQuery.list();
		assertThat( result ).isNotNull();

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();
		assertThat( projection[0] ).as( "SCORE incorrect" ).isInstanceOf( Float.class );
		assertThat( Float.isNaN( (Float) projection[0] ) ).as( "SCORE should not be a NaN" ).isFalse();

		//cleanup
		for ( Object element : s.createQuery( "from " + Employee.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();
	}

	@Test
	void testProjectionInNumericFields() {
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

		Query query = s.getSearchFactory().buildQueryBuilder().forEntity( FootballTeam.class )
				.get()
				.range().onField( "debtInMillions" )
				.from( 600d ).to( 800d )
				.createQuery();

		org.hibernate.search.FullTextQuery hibQuery = s.createFullTextQuery( query, FootballTeam.class );
		hibQuery.setProjection( "nrTitles", "name", "debtInMillions" );

		List<?> result = hibQuery.list();
		assertThat( result ).hasSize( 1 );

		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection ).isNotNull();
		assertThat( projection[0] ).as( "Numeric int Field not projected" ).isInstanceOf( Integer.class );
		assertThat( projection[1] ).as( "String Field not projected" ).isInstanceOf( String.class );
		assertThat( projection[2] ).as( "Numeric double Field not projected" ).isInstanceOf( Double.class );

		assertThat( projection[0] ).isEqualTo( 18 );
		assertThat( projection[1] ).isEqualTo( "Manchester United" );
		assertThat( projection[2] ).isEqualTo( 700.5d );

		//cleanup
		for ( Object element : s.createQuery( "from " + FootballTeam.class.getName() ).list() ) {
			s.delete( element );
		}
		tx.commit();
		s.close();

	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2106")
	void testUnexpectedProjectionConstant() {
		try ( FullTextSession s = Search.getFullTextSession( openSession() ) ) {
			assertThatThrownBy( () -> {
				Query query = s.getSearchFactory().buildQueryBuilder().forEntity( FootballTeam.class )
						.get()
						.range().onField( "debtInMillions" )
						.from( 600d ).to( 800d )
						.createQuery();

				FullTextQuery hibQuery = s.createFullTextQuery( query, FootballTeam.class );
				hibQuery.setProjection( "__HSearch_xyz" );
				hibQuery.list();
				fail();
			} ).isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "HSEARCH000317",
							"Projection constant '__HSearch_xyz' is not supported for this query." );
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

	@Test
	@Tag(Tags.ELASTICSEARCH_SUPPORT_IN_PROGRESS) // HSEARCH-2423 Projecting an unstored field should raise an exception
	void testProjectionOnUnstoredField() throws Exception {
		assertThatThrownBy( () -> {
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
		} ).isInstanceOf( SearchException.class );
	}

	@Test
	void testProjection() throws Exception {
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
		assertThat( result ).isNotNull();
		assertThat( result ).as( "Query with no explicit criteria" ).hasSize( 1 );
		Object[] projection = (Object[]) result.get( 0 );
		assertThat( projection[0] ).as( "id" ).isEqualTo( 1 );
		assertThat( projection[1] ).as( "summary" ).isEqualTo( "La chute de la petite reine a travers les yeux de Festina" );
		assertThat( projection[2] ).as( "mainAuthor.name (embedded objects)" ).isEqualTo( "Emmanuel" );

		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection();
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).as( "Should not trigger projection" ).isInstanceOf( Book.class );

		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( (String[]) null );
		result = hibQuery.list();
		assertThat( result ).isNotNull();
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).as( "Should not trigger projection" ).isInstanceOf( Book.class );

		query = parser.parse( "summary:fleurs" );
		hibQuery = s.createFullTextQuery( query, Book.class );
		hibQuery.setProjection( "id", "summary", "mainAuthor.name" );
		result = hibQuery.list();
		hibQuery.setProjection( "id", "summary", "mainAuthor.name" );
		assertThat( result ).hasSize( 1 );
		projection = (Object[]) result.get( 0 );
		assertThat( projection[2] ).as( "mainAuthor.name" ).isNull();

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
				FootballTeam.class
		};
	}

}
