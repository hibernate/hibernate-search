/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.backend.elasticsearch;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.elasticsearch.ElasticSearchQueries;
import org.hibernate.search.backend.elasticsearch.impl.ElasticSearchIndexManager;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.embedded.Address;
import org.hibernate.search.test.embedded.Country;
import org.hibernate.search.test.embedded.Owner;
import org.hibernate.search.test.embedded.Person;
import org.hibernate.search.test.embedded.State;
import org.hibernate.search.test.embedded.StateCandidate;
import org.hibernate.search.test.embedded.Tower;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Gunnar Morling
 */
public class ElasticSearchTest extends SearchTestBase {

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
				"Object/relational mapping with Hibernate",
				"blah blah blah", 8
		);
		s.persist( article2 );

		ScientificArticle article3 = new ScientificArticle(
				"ORM for beginners",
				"Object/relational mapping with an unknown tool",
				"blah blah blah", 9
		);
		s.persist( article3 );

		ResearchPaper paper1 = new ResearchPaper(
				"Research on Hibernate",
				"Latest research on Hibernate",
				"blah blah blah", 7
		);
		s.persist( paper1 );

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

		tx.commit();
		s.close();
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticSearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
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

		QueryDescriptor query = ElasticSearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		List<?> result = session.createFullTextQuery( query, ScientificArticle.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Latest in ORM", "ORM for dummies" );
		tx.commit();
		s.close();
	}

	@Test
	public void testNumericFieldQuery() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticSearchQueries.fromJson( "{ 'query': { 'range' : { 'wordCount' : { 'gte' : 8 } } } }" );
		List<?> result = session.createFullTextQuery( query ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Latest in ORM", "ORM for beginners" );
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

		query = ElasticSearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'nested' : { " +
							"'path' : 'address'," +
							" 'query': { " +
								"'match' : { " +
									"'address.street' : 'place'" +
								" }" +
							" }" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = ElasticSearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'nested' : { " +
							"'path' : 'address'," +
							" 'query': { " +
								"'match' : { " +
									"'address.ownedBy_name' : 'renting'" +
								" }" +
							" }" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = ElasticSearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'nested' : { " +
							"'path' : 'address'," +
							" 'query': { " +
								"'match' : { " +
									"'address.id' : " + a.getId() +
								" }" +
							" }" +
						" }" +
					" }" +
				" }"
		);
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property by id of embedded", 1, result.size() );

		query = ElasticSearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'nested' : { " +
							"'path' : 'address.country'," +
							" 'query': { " +
								"'match' : { " +
									"'address.country.name' : 'France'" +
								" }" +
							" }" +
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

		query = ElasticSearchQueries.fromJson(
				"{" +
					"'query' : { " +
						"'nested' : { " +
							"'path' : 'address'," +
							" 'query': { " +
								"'match' : { " +
									"'address.ownedBy_name' : 'buckhead'" +
								" }" +
							" }" +
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
	public void testIndexSharing() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticSearchQueries.fromJson( "{ 'query': { 'match' : { 'title' : 'findings' } } }" );
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

		QueryDescriptor query = ElasticSearchQueries.fromJson( "{ 'query': { 'match' : { 'abstract' : 'Hibernate' } } }" );
		List<?> result = session.createFullTextQuery( query, ResearchPaper.class ).list();

		assertThat( result ).onProperty( "title" ).containsOnly( "Research on Hibernate" );
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				ScientificArticle.class, Tower.class, Address.class, Country.class, State.class, StateCandidate.class, ResearchPaper.class, BachelorThesis.class, MasterThesis.class
		};
	}

	@Override
	public void configure(Map<String, Object> settings) {
		settings.put( "hibernate.search.default.indexmanager", ElasticSearchIndexManager.class.getName() );
	}
}
