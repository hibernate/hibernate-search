/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

/**
 * Test the the use of ELasticsearch built-in and custom analyzers.
 *
 * @author Davide D'Alto
 */
public class ElasticsearchAnalyzerIT extends SearchTestBase {

	@Test
	public void testEnglishBuiltInAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = session.beginTransaction();
			Tweet tweet = new Tweet();
			tweet.setEnglishTweet( "Fox" );
			session.persist( tweet );
			tx.commit();
			session.clear();

			TermQuery query = new TermQuery( new Term( "englishTweet", "foxes" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query ).list();
			assertThat( list ).onProperty( "englishTweet" ).containsExactly( tweet.getEnglishTweet() );
		}
	}

	@Test
	public void testWhitespaceBuiltInAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = session.beginTransaction();
			Tweet tweet = new Tweet();
			tweet.setWhitespaceTweet( "What does the fox say?" );
			session.persist( tweet );
			tx.commit();
			session.clear();

			TermQuery query = new TermQuery( new Term( "whitespaceTweet", "fox      say" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query, Tweet.class ).list();
			assertThat( list ).onProperty( "whitespaceTweet" ).containsExactly( tweet.getWhitespaceTweet() );
		}
	}

	@Test
	public void testCustomAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = session.beginTransaction();
			Tweet tweet = new Tweet();
			tweet.setCustomTweet( "Custom" );
			session.persist( tweet );
			tx.commit();
			session.clear();

			TermQuery query = new TermQuery( new Term( "customTweet", "Custom" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query, Tweet.class ).list();
			assertThat( list ).onProperty( "customTweet" ).containsExactly( tweet.getCustomTweet() );
		}
	}

	@Entity
	@Indexed(index = "tweet")
	public static class Tweet {

		@Id
		@GeneratedValue
		private Integer id;

		@Field
		@Analyzer(definition = "english")
		private String englishTweet;

		@Field
		@Analyzer(definition = "whitespace")
		private String whitespaceTweet;

		@Field
		// Defined in the elasticsearch.yml configuration file
		@Analyzer(definition = "custom-analyzer")
		private String customTweet;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getEnglishTweet() {
			return englishTweet;
		}

		public void setEnglishTweet(String englishTweet) {
			this.englishTweet = englishTweet;
		}

		public String getWhitespaceTweet() {
			return whitespaceTweet;
		}

		public void setWhitespaceTweet(String whitespaceTweet) {
			this.whitespaceTweet = whitespaceTweet;
		}

		public String getCustomTweet() {
			return customTweet;
		}

		public void setCustomTweet(String customTweet) {
			this.customTweet = customTweet;
		}

		@Override
		public String toString() {
			return "[" + englishTweet + ", " + whitespaceTweet + "]";
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Tweet.class };
	}
}
