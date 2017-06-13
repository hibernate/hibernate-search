/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.elasticsearch.test.model.Address;
import org.hibernate.search.elasticsearch.test.model.Country;
import org.hibernate.search.elasticsearch.test.model.Owner;
import org.hibernate.search.elasticsearch.test.model.Person;
import org.hibernate.search.elasticsearch.test.model.State;
import org.hibernate.search.elasticsearch.test.model.StateCandidate;
import org.hibernate.search.elasticsearch.test.model.Tower;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		ScientificArticle article1 = new ScientificArticle(
				"ORM for dummies",
				"Object/relational mapping with Hibernate",
				"blah blah blah", 7
		);
		s.persist( article1 );

		ScientificArticle article2 = new ScientificArticle(
				"Latest in ORM",
				"Object/relational mapping with Hibernate - The latest news",
				"blah blah blah", 8
		);
		s.persist( article2 );

		ScientificArticle article3 = new ScientificArticle(
				"ORM for beginners",
				"Object/relational mapping with an unknown tool",
				"blah blah blah", 9
		);
		s.persist( article3 );

		ScientificArticle article4 = new ScientificArticle(
				"High-performance ORM",
				"Tuning persistence with Hibernate",
				"blah blah blah", 10
		);
		s.persist( article4 );

		ScientificArticle article5 = new ScientificArticle(
				"ORM modelling",
				"Modelling your domain model with Hibernate",
				"blah blah blah", 11
		);
		s.persist( article5 );

		ResearchPaper paper1 = new ResearchPaper(
				"Very important research on Hibernate",
				"Latest research on Hibernate",
				"blah blah blah", 7
		);
		s.persist( paper1 );

		ResearchPaper paper2 = new ResearchPaper(
				"Some research",
				"Important Hibernate research",
				"blah blah blah", 7
		);
		s.persist( paper2 );

		BachelorThesis bachelorThesis = new BachelorThesis(
				"Latest findings",
				"blah blah blah"
		);
		s.persist( bachelorThesis );

		MasterThesis masterThesis = new MasterThesis(
				"Great findings",
				"blah blah blah"
		);

		s.persist( masterThesis );

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 0, 0, 0 );
		dob.set( Calendar.MILLISECOND, 0 );

		GolfPlayer hergesheimer = new GolfPlayer.Builder()
			.firstName( "Klaus" )
			.lastName( "Hergesheimer" )
			.active( true )
			.dateOfBirth( dob.getTime() )
			.handicap( 3.4 )
			.puttingStrength( 2.5 )
			.driveWidth( 285 )
			.ranking( 311 )
			.strength( "precision" )
			.strength( "willingness" )
			.strength( "stamina" )
			.build();
		s.persist( hergesheimer );

		tx.commit();
		s.close();
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		tx.commit();
		s.close();
	}

	@Test
	public void testFields() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		List<?> result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly(
				"Latest in ORM",
				"ORM for dummies",
				"High-performance ORM",
				"ORM modelling"
		);
		tx.commit();
		s.close();
	}

	@Test
	public void testNumericFieldQuery() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'range' : { 'wordCount' : { 'gte' : 8, 'lt' : 10 } } } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Latest in ORM", "ORM for beginners" );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2467")
	public void testDateFieldRangeQuery() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		Query query = NumericRangeQuery.newLongRange( "dateOfBirth", -373078800000L, null, true, true );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setSort( new Sort( new SortField( "id", SortField.Type.STRING ) ) )
				.list();

		assertThat( result ).onProperty( "firstName" ).containsOnly( "Klaus" );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedIndexing() throws Exception {
		Tower tower = new Tower();
		tower.setName( "JBoss tower" );
		Address a = new Address();
		a.setStreet( "Tower place" );
		a.getTowers().add( tower );
		tower.setAddress( a );
		Person o = new Owner();
		o.setName( "Atlanta Renting corp" );
		a.setOwnedBy( o );
		o.setAddress( a );
		Country c = new Country();
		c.setName( "France" );
		a.setCountry( c );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryDescriptor query;
		List<?> result;

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'address.street' : 'place'" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'address.ownedBy.name' : 'renting'" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'address.id' : " + a.getId() +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property by id of embedded", 1, result.size() );

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'address.country.name' : 'France'" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property with 2 levels of embedded", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		Address address = s.get( Address.class, a.getId() );
		address.getOwnedBy().setName( "Buckhead community" );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'address.ownedBy.name' : 'buckhead'" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();
	}

	@Test
	public void testEmbeddedIndexingOfElementCollection() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		FullTextSession session = Search.getFullTextSession( s );
		QueryDescriptor query;
		List<?> result;

		query = ElasticsearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'match' : { " +
							"'strengths' : 'willingness'" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, GolfPlayer.class ).list();
		assertEquals( "unable to find property in embedded element collection", 1, result.size() );

		tx.commit();
		s.close();
	}

	@Test
	public void testIndexSharing() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'title' : 'findings' } } }" );
		List<?> result = session.createFullTextQuery( query, MasterThesis.class, BachelorThesis.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Great findings", "Latest findings" );
		tx.commit();
		s.close();
	}

	@Test
	public void testRestrictionByType() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		List<?> result = session.createFullTextQuery( query, ResearchPaper.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Very important research on Hibernate", "Some research" );
		tx.commit();
		s.close();
	}

	@Test
	public void testFirstResultAndMaxResults() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		List<?> result = session.createFullTextQuery( query, ScientificArticle.class )
				.setFirstResult( 1 )
				.setMaxResults( 2 )
				.setSort( new Sort( new SortField( "id", SortField.Type.STRING, false ) ) )
				.list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Latest in ORM", "High-performance ORM" );
		tx.commit();
		s.close();
	}

	@Test
	public void testGetResultSize() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		FullTextQuery fullTextQuery = session.createFullTextQuery( query, ScientificArticle.class );

		assertThat( fullTextQuery.getResultSize() ).isEqualTo( 4 );

		tx.commit();
		s.close();
	}

	@Test
	public void testScroll() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		FullTextQuery fullTextQuery = session.createFullTextQuery( query, ScientificArticle.class )
				.setSort( new Sort( new SortField( "id", SortField.Type.STRING, false ) ) );

		ScrollableResults scrollableResults = fullTextQuery.scroll();

		assertEquals( -1, scrollableResults.getRowNumber() );
		assertTrue( scrollableResults.last() );
		assertEquals( 3, scrollableResults.getRowNumber() );
		scrollableResults.beforeFirst();

		List<ScientificArticle> articles = new ArrayList<>();
		while ( scrollableResults.next() ) {
			articles.add( (ScientificArticle) scrollableResults.get()[0] );
		}
		scrollableResults.close();

		assertThat( articles ).onProperty( "title" ).containsExactly(
				"ORM for dummies",
				"Latest in ORM",
				"High-performance ORM",
				"ORM modelling"
		);

		fullTextQuery = session.createFullTextQuery( query, ScientificArticle.class )
				.setSort( new Sort( new SortField( "id", SortField.Type.STRING, false ) ) );

		scrollableResults = fullTextQuery
				.setFirstResult( 1 )
				.setMaxResults( 2 )
				.scroll();

		assertEquals( -1, scrollableResults.getRowNumber() );
		assertTrue( scrollableResults.last() );
		assertEquals( 1, scrollableResults.getRowNumber() );
		scrollableResults.beforeFirst();

		articles = new ArrayList<>();
		while ( scrollableResults.next() ) {
			articles.add( (ScientificArticle) scrollableResults.get()[0] );
		}
		scrollableResults.close();

		assertThat( articles ).onProperty( "title" ).containsExactly(
				"Latest in ORM",
				"High-performance ORM"
		);

		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2253")
	public void testSort() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );

		// by title, ascending
		List<?> result = session.createFullTextQuery( query, ScientificArticle.class )
				.setSort( new Sort( new SortField( "title", SortField.Type.STRING, false ) ) )
				.list();

		assertThat( result ).onProperty( "title" ).containsExactly(
				"High-performance ORM",
				"Latest in ORM",
				"ORM for dummies",
				"ORM modelling"
		);

		// By id, descending
		result = session.createFullTextQuery( query, ScientificArticle.class )
				.setSort( new Sort( new SortField( "id", SortField.Type.STRING, true ) ) )
				.list();

		assertThat( result ).onProperty( "id" ).containsExactly(
				5L,
				4L,
				2L,
				1L
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testFieldBoost() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson(
				"{" +
					"'query': {" +
						"'bool' : {" +
							"'should' : [" +
								"{ 'match' : { 'abstract' : 'important' } }," +
								"{ 'match' : { 'title' : 'important' } }" +
							"]" +
						"}" +
					"}" +
				"}"
		);

		List<?> result = session.createFullTextQuery( query, ResearchPaper.class ).list();

		assertThat( result ).onProperty( "title" ).containsExactly(
				"Very important research on Hibernate",
				"Some research"
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testQueryStringQuery() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "abstract:Hibernate" );
		List<?> result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly(
				"Latest in ORM",
				"ORM for dummies",
				"High-performance ORM",
				"ORM modelling"
		);

		query = ElasticsearchQueries.fromQueryString( "abstract:important OR title:important" );
		result = session.createFullTextQuery( query, ResearchPaper.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly(
				"Very important research on Hibernate",
				"Some research"
		);

		query = ElasticsearchQueries.fromQueryString( "wordCount:[8 TO 10}" );
		result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Latest in ORM", "ORM for beginners" );

		tx.commit();
		s.close();
	}

	@Test
	public void testProjection() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromQueryString( "lastName:Hergesheimer" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection(
						ElasticsearchProjectionConstants.ID,
						ElasticsearchProjectionConstants.OBJECT_CLASS,
						ElasticsearchProjectionConstants.SCORE,
						ElasticsearchProjectionConstants.THIS,
						"firstName",
						"lastName",
						"active",
						"dateOfBirth",
						"handicap",
						"driveWidth",
						"ranking.value",
						ElasticsearchProjectionConstants.TOOK,
						ElasticsearchProjectionConstants.TIMED_OUT
				)
				.list();

		assertThat( result ).hasSize( 1 );

		Calendar dob = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
		dob.set( 1958, 3, 7, 0, 0, 0 );
		dob.set( Calendar.MILLISECOND, 0 );

		Object[] projection = (Object[]) result.iterator().next();
		assertThat( projection[0] ).describedAs( "id" ).isEqualTo( 1L );
		assertThat( projection[1] ).describedAs( "object class" ).isEqualTo( GolfPlayer.class );
		assertThat( projection[2] ).describedAs( "score" ).isInstanceOf( Float.class );
		assertThat( projection[3] ).describedAs( "this" ).isInstanceOf( GolfPlayer.class );
		assertThat( ( (GolfPlayer) projection[3] ).getId() ).isEqualTo( 1L );
		assertThat( projection[4] ).describedAs( "firstName" ).isEqualTo( "Klaus" );
		assertThat( projection[5] ).describedAs( "lastName" ).isEqualTo( "Hergesheimer" );
		assertThat( projection[6] ).describedAs( "active" ).isEqualTo( true );
		assertThat( projection[7] ).describedAs( "dateOfBirth" ).isEqualTo( dob.getTime() );
		assertThat( projection[8] ).describedAs( "handicap" ).isEqualTo( 3.4D );
		assertThat( projection[9] ).describedAs( "driveWidth" ).isEqualTo( 285 );
		assertThat( projection[10] ).describedAs( "ranking value" ).isEqualTo( BigInteger.valueOf( 311 ) );
		assertThat( projection[11] ).describedAs( "took" ).isInstanceOf( Integer.class );
		assertThat( (Integer) projection[11] ).describedAs( "took" ).isGreaterThanOrEqualTo( 0 );
		assertThat( projection[12] ).describedAs( "timeout" ).isEqualTo( Boolean.FALSE );

		tx.commit();
		s.close();
	}

	@Test
	public void testQueryById() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson(
				"{" +
					"'query': {" +
						"'ids' : {" +
							"'values' : ['1', '3']" +
						"}" +
					"}" +
				"}"
		);

		List<?> result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly(
				"ORM for dummies",
				"ORM for beginners"
		);

		query = ElasticsearchQueries.fromQueryString( "_id:1 OR _id:3" );
		result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly(
				"ORM for dummies",
				"ORM for beginners"
		);

		tx.commit();
		s.close();
	}

	@Test
	public void testStringMappedNumericProperty() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'puttingStrength' : '2.5' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.ID, "puttingStrength" )
				.list();

		assertThat( result ).hasSize( 1 );
		Object[] projection = (Object[]) result.iterator().next();

		assertThat( projection[0] ).isEqualTo( 1L );
		assertThat( projection[1] ).isEqualTo( 2.5D );

		tx.commit();
		s.close();
	}

	@Test
	public void testBooleanProperty() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'term' : { 'active' : 'true' } } }" );
		List<?> result = session.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.ID )
				.list();

		assertThat( result ).hasSize( 1 );
		Object[] projection = (Object[]) result.iterator().next();

		assertThat( projection[0] ).isEqualTo( 1L );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ScientificArticle.class, Tower.class, Address.class, Country.class, State.class, StateCandidate.class, ResearchPaper.class,
				BachelorThesis.class, MasterThesis.class, GolfPlayer.class, GolfCourse.class, Hole.class };
	}
}
