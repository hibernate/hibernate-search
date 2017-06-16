/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.CalendarBridge;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.SortableField;
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
			QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.phrase()
						.withSlop( 2 )
						.onField( "message" )
						.sentence( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_phrase':{'message':{'query':'A very important matter','slop':2}}}}", queryString );


			queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Letter.class )
					.overridesForField( "message", "english" ).get();
			query = queryBuilder
					.phrase()
						.withSlop( 2 )
						.onField( "message" )
						.sentence( "A very important matter" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_phrase':{'message':{'query':'A very important matter','slop':2}}}}", queryString );


			queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Letter.class )
					.overridesForField( "message", "french" ).get();
			query = queryBuilder
					.phrase()
						.withSlop( 2 )
						.onField( "message" )
						.sentence( "A very important matter" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match_phrase':{'message':{'query':'A very important matter','slop':2,'analyzer':'french'}}}}", queryString );
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
			QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "message" )
						.matching( "A very important matter" )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter'}}}}", queryString );

			queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Letter.class )
					.overridesForField( "message", "english" ).get();
			query = queryBuilder
					.keyword()
						.onField( "message" )
						.matching( "A very important matter" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter'}}}}", queryString );

			queryBuilder = fullTextSession.getSearchFactory()
					.buildQueryBuilder().forEntity( Letter.class )
					.overridesForField( "message", "french" ).get();
			query = queryBuilder
					.keyword()
						.onField( "message" )
						.matching( "A very important matter" )
					.createQuery();

			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','analyzer':'french'}}}}", queryString );
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
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','fuzziness':2}}}}", queryString );
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
			assertJsonEquals( "{'query':{'match':{'message':{'query':'A very important matter','boost':2.0}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordBoolean() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "personal" )
						.matching( true )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'term':{'personal':{'value':'true'}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordFloat() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.keyword()
						.onField( "shippingCost" )
						.matching( 0.40f )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'range':{'shippingCost':{'gte':0.4,'lte':0.4}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordDate() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
			date.set( 1958, 3, 7, 5, 5, 5 );
			date.set( Calendar.MILLISECOND, 0 );

			Query query = queryBuilder
					.keyword()
						.onField( "dateWritten" )
						.matching( date.getTime() )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'term':{'dateWritten':{'value':'1958-04-07T00:00:00Z'}}}}", queryString );
		}
	}

	@Test
	public void testDSLKeywordCalendar() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ), Locale.ENGLISH );
			calendar.set( 1958, 3, 7, 5, 5, 5 );
			calendar.set( Calendar.MILLISECOND, 0 );

			Query query = queryBuilder
					.keyword()
						.onField( "dateSent" )
						.matching( calendar )
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'term':{'dateSent':{'value':'1958-04-07T00:00:00Z'}}}}", queryString );
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
			assertJsonEquals( "{'query':{'match_phrase':{'signature':{'query':'Gunnar Morling'}}}}", queryString );
		}
	}

	@Test
	public void testDSLSortNativeSimpleString() {
		try ( Session session = openSession() ) {
			Letter letter1 = new Letter();
			Letter letter2 = new Letter();
			persist( session, letter1 );
			persist( session, letter2 );

			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.all()
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			Sort sort = queryBuilder.sort().byNative( "idSort", "\"desc\"" ).createSort();
			fullTextQuery.setSort( sort );
			List<?> list = fullTextQuery.list();
			assertThat( list ).onProperty( "id" )
					.containsExactly( letter2.getId(), letter1.getId() );

			// Make sure the assertion above didn't just pass by chance
			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			sort = queryBuilder.sort().byNative( "idSort", "\"asc\"" ).createSort();
			fullTextQuery.setSort( sort );
			list = fullTextQuery.list();
			assertThat( list ).onProperty( "id" )
					.containsExactly( letter1.getId(), letter2.getId() );
		}
	}

	@Test
	public void testDSLSortNativeMap() {
		try ( Session session = openSession() ) {
			Letter letter1 = new Letter();
			Letter letter2 = new Letter();
			persist( session, letter1 );
			persist( session, letter2 );

			FullTextSession fullTextSession = Search.getFullTextSession( session );
			final QueryBuilder queryBuilder = queryBuilder( fullTextSession );

			Query query = queryBuilder
					.all()
					.createQuery();

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			Sort sort = queryBuilder.sort().byNative( "idSort", "{\"order\":\"desc\"}" ).createSort();
			fullTextQuery.setSort( sort );
			List<?> list = fullTextQuery.list();
			assertThat( list ).onProperty( "id" )
					.containsExactly( letter2.getId(), letter1.getId() );

			// Make sure the first assertion above didn't just pass by chance
			fullTextQuery = fullTextSession.createFullTextQuery( query, Letter.class );
			sort = queryBuilder.sort().byNative( "idSort", "{\"order\":\"asc\"}" ).createSort();
			fullTextQuery.setSort( sort );
			list = fullTextQuery.list();
			assertThat( list ).onProperty( "id" )
					.containsExactly( letter1.getId(), letter2.getId() );
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

	private void persist(Session session, Letter letter) {
		Transaction tx = session.beginTransaction();
		session.persist( letter );
		tx.commit();
		session.clear();
	}

	@Entity
	@Indexed
	public static class Letter {

		@Id
		@GeneratedValue
		@Field(name = "idSort")
		@SortableField(forField = "idSort")
		private Integer id;

		@Field
		@Analyzer(definition = "english")
		// Hack to be able to use a query-only analyzer
		@Field(name = "message_french", analyzer = @Analyzer(definition = "french"))
		private String message;

		@Field
		private String signature;

		@Field
		@DateBridge(resolution = Resolution.DAY)
		private Date dateWritten;

		@Field
		@CalendarBridge(resolution = Resolution.DAY)
		private Calendar dateSent;

		@Field
		private boolean personal;

		@Field
		private float shippingCost;

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


		public Date getDateWritten() {
			return dateWritten;
		}


		public void setDateWritten(Date dateWritten) {
			this.dateWritten = dateWritten;
		}


		public Calendar getDateSent() {
			return dateSent;
		}


		public void setDateSent(Calendar dateSent) {
			this.dateSent = dateSent;
		}

	}
}
