/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.elasticsearch.testutil.JsonHelper.assertJsonEquals;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

/**
 * Test the Query DSL with Elasticsearch
 *
 * @author Davide D'Alto
 */
public class ElasticsearchDSLIT extends SearchTestBase {

	@Test
	public void testDSLMatchAll() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.all()
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_all':{}}}", queryString );
		}
	}

	@Test
	public void testDSLPhrase() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.phrase()
						.withSlop( 2 )
						.onField( "message" )
						.sentence( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_phrase':{'message':{'query':'A very important matter','slop':2,'analyzer':'english'}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordIgnoringAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "message" )
							.ignoreAnalyzer()
						.matching( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'term':{'message':{'value':'A very important matter'}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeyword() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "message" )
						.matching( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','analyzer':'english'}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordWithFuzziness() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.fuzzy()
							.withEditDistanceUpTo( 2 )
						.onField( "message" )
						.matching( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','analyzer':'english','fuzziness':2}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordWithBoost() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "message" )
						.boostedTo( 2.0f )
						.matching( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','analyzer':'english','boost':2.0}}}}", queryString );
		}
	}

	@Test
	public void testDSLPhraseQueryWithoutAnalyzer() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.phrase()
						.onField( "signature" )
						.sentence( "Gunnar Morling" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_phrase':{'signature':{'query':'Gunnar Morling','analyzer':'default'}}}}", queryString );
		}
	}

	private QueryBuilder queryBuilder(FullTextSession fullTextSession) {
		final QueryBuilder tweetQueryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity( Letter.class ).get();
		return tweetQueryBuilder;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Letter.class };
	}

	@Entity
	@Indexed
	public static class Letter {

		@Id
		@GeneratedValue
		private Integer id;

		@Field
		@Analyzer(definition = "english")
		private String message;

		@Field
		private String signature;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getSignature() {
			return signature;
		}

		public void setSignature(String signature) {
			this.signature = signature;
		}
	}
}
