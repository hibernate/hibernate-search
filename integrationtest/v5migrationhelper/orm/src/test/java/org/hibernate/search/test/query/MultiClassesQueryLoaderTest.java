/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
class MultiClassesQueryLoaderTest extends SearchTestBase {

	private Query luceneQuery;

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();

			// used for the filtering tests
			Author author = new Author();
			author.setName( "Moo Cow" );
			Music music = new Music();
			music.addAuthor( author );
			music.setTitle( "The moo moo mooing under the stars" );
			Book book = new Book();
			book.setBody( "This is the story of the Moo Cow, who sang the moo moo moo at night" );
			book.setId( 1 );
			session.persist( book );
			session.persist( author );
			session.persist( music );

			// used for the not found test
			Author charles = new Author();
			charles.setName( "Charles Dickens" );
			session.persist( charles );

			tx.commit();
		}

		QueryParser parser = new QueryParser( "title", TestConstants.keywordAnalyzer );
		luceneQuery = parser.parse( "name:moo OR title:moo OR body:moo" );
	}

	@Test
	void testObjectNotFound() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						Statement statement = connection.createStatement();
						statement.executeUpdate( "DELETE FROM Author where name = 'Charles Dickens'" );
						statement.close();
					}
				}
		);

		FullTextSession s = Search.getFullTextSession( session );
		QueryParser parser = new QueryParser( "title", TestConstants.keywordAnalyzer );
		Query query = parser.parse( "name:charles" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Author.class, Music.class );
		List result = hibQuery.list();
		assertThat( result ).as( "Should have returned no author" ).isEmpty();

		tx.commit();
		s.close();
	}

	@Test
	void testObjectTypeFilteringSingleClass() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Music.class );
		List result = fullTextQuery.list();
		assertThat( result ).as( "Should match the music only" ).hasSize( 1 );
		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testObjectTypeFilteringTwoClasses() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery, Author.class, Music.class );
		List result = fullTextQuery.list();
		assertThat( result ).as( "Should match the author and music only" ).hasSize( 2 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testObjectTypeFilteringThreeClasses() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
				luceneQuery,
				Author.class,
				Music.class,
				Book.class
		);
		List result = fullTextQuery.list();

		assertThat( result ).as( "Should match the author, music and book" ).hasSize( 3 );
		fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery );
		result = fullTextQuery.list();
		assertThat( result ).as( "Should match all types" ).hasSize( 3 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	void testImplicitObjectTypeFiltering() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( luceneQuery );
		List result = fullTextQuery.list();
		assertThat( result ).as( "Should match all types" ).hasSize( 3 );

		tx.commit();
		fullTextSession.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class,
				Music.class,
				Book.class
		};
	}

}
