/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.search.elasticsearch.testutil.JsonHelper.assertJsonEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PrefixQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Lucene query to Elasticsearch conversion.
 *
 * Note that most of the current conversions are tested by the {@link ElasticsearchDSLIT} test.
 *
 * @author Guillaume Smet
 */
public class ToElasticsearchDSLIT extends SearchTestBase {

	@Before
	public void setupTestData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Letter letter = new Letter( "Important letter", "Gunnar Morling" );
		s.persist( letter );

		letter = new Letter( "Dear Sanne...", "Gunnar Morling" );
		s.persist( letter );

		tx.commit();
		s.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDSLPhraseQueryWithoutAnalyzer() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );

			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(
					new PrefixQuery( new Term( "message", "import" ) ), Letter.class );

			String queryString = fullTextQuery.getQueryString();
			assertJsonEquals( "{'query':{'prefix':{'message':{'value':'import'}}}}", queryString );

			List<Letter> letters = fullTextQuery.list();

			assertThat( letters ).hasSize( 1 );
			assertThat( letters ).onProperty( "message" ).containsExactly( "Important letter" );
		}
	}

	@After
	public void deleteTestData() {
		Session s = openSession();
		FullTextSession session = Search.getFullTextSession( s );
		Transaction tx = s.beginTransaction();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = session.createFullTextQuery( query, Letter.class ).list();

		for ( Object entity : result ) {
			session.delete( entity );
		}

		tx.commit();
		s.close();
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

		@Field
		@DateBridge(resolution = Resolution.DAY)
		private Date dateWritten;

		@Field
		@DateBridge(resolution = Resolution.DAY)
		private Calendar dateSent;

		@Field
		private boolean personal;

		@Field
		private float shippingCost;

		public Letter() {
		}

		public Letter(String message, String signature) {
			this.message = message;
			this.signature = signature;
		}

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
