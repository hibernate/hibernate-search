/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;


/**
 * @author Gunnar Morling
 */
public class CombiningLuceneAndElasticsearchIT extends SearchTestBase {

	private final QueryParser queryParser = new QueryParser( "id", TestConstants.simpleAnalyzer );

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		ElasticsearchIndexedEntity paper = new ElasticsearchIndexedEntity( "important research" );
		s.persist( paper );

		LuceneIndexedEntity luceneIndexedEntity = new LuceneIndexedEntity( "The tales of Bob" );
		s.persist( luceneIndexedEntity );

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
				ElasticsearchIndexedEntity.class
		).list();

		assertThat( result ).extracting( "title" ).containsExactly( "important research" );

		result = session.createFullTextQuery(
				queryParser.parse( "title:tales" ),
				LuceneIndexedEntity.class
		).list();

		assertThat( result ).extracting( "title" ).containsExactly( "The tales of Bob" );

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
					LuceneIndexedEntity.class
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
			session.createFullTextQuery( new NotTranslatableQuery(), ElasticsearchIndexedEntity.class ).list();
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
		List<?> result = session.createFullTextQuery( query, ElasticsearchIndexedEntity.class ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		result = session.createFullTextQuery( new MatchAllDocsQuery(), LuceneIndexedEntity.class ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}


		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { ElasticsearchIndexedEntity.class, LuceneIndexedEntity.class };
	}

	@Override
	public void configure(Map<String, Object> settings) {
		// default is ES
		settings.put( "hibernate.search.luceneIndex.indexmanager", "directory-based" );
		settings.put( "hibernate.search.luceneIndex.directory_provider", "local-heap" );
	}

	private static class NotTranslatableQuery extends Query {

		@Override
		public String toString(String field) {
			return getClass().getName();
		}

	}

	@Entity
	@Indexed(index = "elasticsearchIndex")
	public static class ElasticsearchIndexedEntity {

		@Id
		@GeneratedValue
		@DocumentId
		private Long id;

		@Field
		private String title;

		ElasticsearchIndexedEntity() {
		}

		public ElasticsearchIndexedEntity(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity
	@Indexed(index = "luceneIndex")
	public static class LuceneIndexedEntity {

		@Id
		@GeneratedValue
		@DocumentId
		private Long id;

		@Field
		private String title;

		LuceneIndexedEntity() {
		}

		public LuceneIndexedEntity(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}


}
