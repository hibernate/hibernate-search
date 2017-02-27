/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

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
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonParser;

/**
 * Test the use of Elasticsearch built-in and server-defined, custom analyzers,
 * <strong>without</strong> using {@link AnalyzerDef}.
 *
 * @author Davide D'Alto
 */
public class ElasticsearchAnalyzerIT extends SearchTestBase {

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	@Override
	@Before
	public void setUp() throws Exception {
		// Make sure automatically created indexes will have the "server-defined-custom-analyzer" analyzer definition
		elasticsearchClient.template( "server-defined-custom-analyzer" )
				.create(
						"*",
						new JsonParser().parse(
								"{"
									+ "'index': {"
										+ "'analysis': {"
											+ "'analyzer': {"
												+ "'server-defined-custom-analyzer': {"
														+ "'char_filter': ['html_strip'],"
														+ "'tokenizer': 'standard',"
														+ "'filter': ['server-defined-custom-filter', 'lowercase']"
												+ "}"
											+ "},"
											+ "'filter': {"
												+ "'server-defined-custom-filter': {"
														+ "'type': 'stop',"
														+ "'stopwords': ['test1', 'close']"
												+ "}"
											+ "}"
										+ "}"
									+ "}"
								+ "}"
						)
								.getAsJsonObject()
					);

		super.setUp();
	}

	@Test
	public void testDefaultAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setDefaultTweet( "the Foxes" );
			tweet( session, tweet );

			TermQuery query = new TermQuery( new Term( "defaultTweet", "fox" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query ).list();
			assertThat( list ).as( "It should not find the tweet without a defined analyzer" ).isEmpty();
		}
	}

	@Test
	public void testEnglishBuiltInAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setEnglishTweet( "the Foxes" );
			tweet( session, tweet );

			TermQuery query = new TermQuery( new Term( "englishTweet", "fox" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query ).list();
			assertThat( list ).onProperty( "englishTweet" ).containsExactly( tweet.getEnglishTweet() );
		}
	}

	@Test
	public void testWhitespaceBuiltInAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setWhitespaceTweet( "What does the fox say?" );
			tweet( session, tweet );

			TermQuery query = new TermQuery( new Term( "whitespaceTweet", "fox" ) );
			@SuppressWarnings("unchecked")
			List<Tweet> list = fullTextSession.createFullTextQuery( query, Tweet.class ).list();
			assertThat( list ).onProperty( "whitespaceTweet" ).containsExactly( tweet.getWhitespaceTweet() );
		}
	}

	@Test
	public void testCustomAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setCustomTweet( "close OPEN SOURCE test1" );
			tweet( session, tweet );

			@SuppressWarnings("unchecked")
			List<Tweet> expectedResult = fullTextSession.createFullTextQuery( termQuery( "customTweet", "open" ), Tweet.class ).list();
			assertThat( expectedResult ).onProperty( "customTweet" ).containsExactly( tweet.getCustomTweet() );

			@SuppressWarnings("unchecked")
			List<Tweet> expectedEmpty = fullTextSession.createFullTextQuery( termQuery( "customTweet", "CLOSE" ), Tweet.class ).list();
			assertThat( expectedEmpty ).as( "Custom analyzer or filter not applied" ).isEmpty();
		}
	}

	@Test
	public void testMultipleFieldsCustomAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setMultipleTweets( "close OPEN SOURCE test1" );
			tweet( session, tweet );

			@SuppressWarnings("unchecked")
			List<Tweet> expectedResult = fullTextSession.createFullTextQuery( termQuery( "tweetWithCustom", "open" ), Tweet.class ).list();
			assertThat( expectedResult ).onProperty( "multipleTweets" ).containsExactly( tweet.getMultipleTweets() );

			@SuppressWarnings("unchecked")
			List<Tweet> expectedEmpty = fullTextSession.createFullTextQuery( termQuery( "tweetWithCustom", "CLOSE" ), Tweet.class ).list();
			assertThat( expectedEmpty ).as( "Custom analyzer or filter not applied" ).isEmpty();
		}
	}

	@Test
	public void testMultipleFieldIgnoreAnalyzer() throws Exception {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Tweet tweet = new Tweet();
			tweet.setMultipleTweets( "close OPEN SOURCE test1" );
			tweet( session, tweet );

			@SuppressWarnings("unchecked")
			List<Tweet> expectedResult = fullTextSession.createFullTextQuery( termQuery( "tweetNotAnalyzed", "close OPEN SOURCE test1" ), Tweet.class ).list();
			assertThat( expectedResult ).onProperty( "multipleTweets" ).containsExactly( tweet.getMultipleTweets() );
		}
	}

	private TermQuery termQuery(String fld, String text) {
		TermQuery query = new TermQuery( new Term( fld, text ) );
		return query;
	}

	private void tweet(Session session, Tweet tweet) {
		Transaction tx = session.beginTransaction();
		session.persist( tweet );
		tx.commit();
		session.clear();
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
		@Analyzer(definition = "server-defined-custom-analyzer")
		private String customTweet;

		@Field(name = "tweetNotAnalyzed", analyze = Analyze.NO, store = Store.YES)
		@Field(name = "tweetWithCustom", analyzer = @Analyzer(definition = "server-defined-custom-analyzer") )
		private String multipleTweets;

		private String defaultAnalyzer;

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

		public String getDefaultAnalyzer() {
			return defaultAnalyzer;
		}

		public void setDefaultTweet(String defaultAnalyzer) {
			this.defaultAnalyzer = defaultAnalyzer;
		}

		public String getMultipleTweets() {
			return multipleTweets;
		}

		public void setMultipleTweets(String multipleMessage) {
			this.multipleTweets = multipleMessage;
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
