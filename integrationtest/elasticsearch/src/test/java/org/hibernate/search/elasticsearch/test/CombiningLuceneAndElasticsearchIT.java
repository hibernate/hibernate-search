/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;


/**
 * @author Gunnar Morling
 */
public class CombiningLuceneAndElasticsearchIT extends SearchTestBase {

	private final QueryParser queryParser = new QueryParser( "id", TestConstants.simpleAnalyzer );

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		ResearchPaper paper = new ResearchPaper( "important research", "latest findings", "Once upon a time...", 100 );
		s.persist( paper );

		ComicBook comicBook = new ComicBook( "The tales of Bob", "Once upon a time..." );
		s.persist( comicBook );

		tx.commit();
		s.close();
	}

	@Test
	public void canUseLuceneAndElasticsearchForDifferentEntities() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		List<?> result = session.createFullTextQuery(
				ElasticsearchQueries.fromQueryString( "title:important" ),
				ResearchPaper.class
		).list();

		assertThat( result ).onProperty( "title" ).containsExactly( "important research" );

		result = session.createFullTextQuery(
				queryParser.parse( "title:tales" ),
				ComicBook.class
		).list();

		assertThat( result ).onProperty( "title" ).containsExactly( "The tales of Bob" );

		tx.commit();
		s.close();
	}

	@Test
	public void cannotUseElasticsearchQueryWithLuceneIndexedEntity() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		try {
			session.createFullTextQuery(
					ElasticsearchQueries.fromQueryString( "title:tales" ),
					ComicBook.class
			).list();

			fail( "Expected exception wasn't raised" );
		}
		catch (SearchException se) {
			assertThat( se.getMessage() ).contains( "HSEARCH400001" );
		}
		finally {
			tx.commit();
			s.close();
		}
	}

	@Test
	public void cannotUseLuceneQueryWithElasticsearchIndexedEntity() throws Exception {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		try {
			session.createFullTextQuery( new NotTranslatableQuery(), ResearchPaper.class ).list();
			fail( "Expected exception wasn't raised" );
		}
		catch (SearchException se) {
			assertThat( se.getMessage() ).contains( "HSEARCH400002" );
		}
		finally {
			tx.commit();
			s.close();
		}
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query, ResearchPaper.class ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		result = session.createFullTextQuery( new MatchAllDocsQuery(), ComicBook.class ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}


		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ResearchPaper.class, ComicBook.class };
	}

	@Override
	public void configure(Map<String, Object> settings) {
		// default is ES
		settings.put( "hibernate.search.comic_book.indexmanager", "directory-based" );
		settings.put( "hibernate.search.comic_book.directory_provider", "local-heap" );
	}

	private static class NotTranslatableQuery extends Query {

		@Override
		public String toString(String field) {
			return getClass().getName();
		}

	}
}
