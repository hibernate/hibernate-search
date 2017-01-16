/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.FullTextSessionBuilder;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // Custom analyzer implementations cannot be used with Elasticsearch
public class AnalyzerTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	@Test
	public void testAnalyzerDiscriminator() throws Exception {
		Article germanArticle = new Article();
		germanArticle.setLanguage( "de" );
		germanArticle.setText( "aufeinanderschl\u00FCgen" );
		Set<Article> references = new HashSet<Article>();
		references.add( germanArticle );

		Article englishArticle = new Article();
		englishArticle.setLanguage( "en" );
		englishArticle.setText( "acknowledgment" );
		englishArticle.setReferences( references );

		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( englishArticle );
		tx.commit();

		tx = s.beginTransaction();

		// at query time we use a standard analyzer. We explicitly search for tokens which can only be found if the
		// right language specific stemmer was used at index time
		QueryParser parser = new QueryParser( "references.text", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "aufeinanderschlug" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		assertEquals( 1, query.getResultSize() );

		parser = new QueryParser( "text", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "acknowledg" );
		query = s.createFullTextQuery( luceneQuery );
		assertEquals( 1, query.getResultSize() );

		tx.commit();
		s.close();
	}

	@Test
	public void testMultipleAnalyzerDiscriminatorDefinitions() {
		FullTextSessionBuilder builder = new FullTextSessionBuilder();
		builder.addAnnotatedClass( BlogEntry.class );
		try {
			builder.build();
			fail();
		}
		catch (SearchException e) {
			assertTrue(
					"Wrong error message",
					e.getMessage().startsWith( "Multiple AnalyzerDiscriminator defined in the same class hierarchy" )
			);
		}
	}

	@Test
	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setEntity( "Entity" );
		en.setField( "Field" );
		en.setProperty( "Property" );
		en.setComponent( new MyComponent() );
		en.getComponent().setComponentProperty( "component property" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( en );
		tx.commit();

		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "entity:alarm" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "property:cat" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "field:energy" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "component.componentProperty:noise" );
		query = s.createFullTextQuery( luceneQuery, MyEntity.class );
		assertEquals( 1, query.getResultSize() );

		s.delete( query.getSingleResult() );
		tx.commit();

		s.close();
	}

	@Test
	public void testScopedAnalyzersFromSearchFactory() throws Exception {
		FullTextSession session = Search.getFullTextSession( openSession() );
		SearchFactory searchFactory = session.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MyEntity.class );

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "entity", "" );
		assertTokensEqual( tokens, new String[] { "alarm", "dog", "performance" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "property", "" );
		assertTokensEqual( tokens, new String[] { "sound", "cat", "speed" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "field", "" );
		assertTokensEqual( tokens, new String[] { "music", "elephant", "energy" } );

		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "component.componentProperty", "" );
		assertTokensEqual( tokens, new String[] { "noise", "mouse", "light" } );

		// test border cases
		try {
			searchFactory.getAnalyzer( (Class) null );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		try {
			searchFactory.getAnalyzer( String.class );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		session.close();
	}

	@Test
	public void testNotAnalyzedFieldAndScopedAnalyzer() throws Exception {
		FullTextSession session = Search.getFullTextSession( openSession() );
		SearchFactory searchFactory = session.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MyEntity.class );

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "notAnalyzed", "pass through" );
		assertTokensEqual( tokens, new String[] { "pass through" } );

		session.close();
	}

	public static void assertTokensEqual(Token[] tokens, String[] strings) {
		Assert.assertEquals( strings.length, tokens.length );

		for ( int i = 0; i < tokens.length; i++ ) {
			Assert.assertEquals( "index " + i, strings[i], AnalyzerUtils.getTermText( tokens[i] ) );
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class, Article.class };
	}
}
