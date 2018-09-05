/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.common;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Category(SkipOnElasticsearch.class) // Custom analyzer implementations cannot be used with Elasticsearch
public class AnalyzerTest {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final IndexedTypeIdentifier MY_ENTITY_TYPE_ID = PojoIndexedTypeIdentifier.convertFromLegacy( MyEntity.class );

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( MyEntity.class, Article.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	public void testAnalyzerDiscriminator() throws Exception {
		Article germanArticle = new Article();
		germanArticle.setId( 1 );
		germanArticle.setLanguage( "de" );
		germanArticle.setText( "aufeinanderschl\u00FCgen" );
		Set<Article> references = new HashSet<Article>();
		references.add( germanArticle );

		Article englishArticle = new Article();
		englishArticle.setId( 2 );
		englishArticle.setLanguage( "en" );
		englishArticle.setText( "acknowledgment" );
		englishArticle.setReferences( references );

		helper.index( englishArticle, germanArticle );

		// at query time we use a standard analyzer. We explicitly search for tokens which can only be found if the
		// right language specific stemmer was used at index time
		QueryParser parser = new QueryParser( "references.text", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "aufeinanderschlug" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 2 );

		parser = new QueryParser( "text", TestConstants.standardAnalyzer );
		luceneQuery = parser.parse( "acknowledg" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 2 );
	}

	@Test
	public void testScopedAnalyzers() throws Exception {
		MyEntity en = new MyEntity();
		en.setId( 1 );
		en.setEntity( "Entity" );
		en.setField( "Field" );
		en.setProperty( "Property" );
		en.setComponent( new MyComponent() );
		en.getComponent().setComponentProperty( "component property" );

		helper.index( en );

		QueryParser parser = new QueryParser( "id", TestConstants.standardAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "entity:alarm" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 1 );

		luceneQuery = parser.parse( "property:cat" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 1 );

		luceneQuery = parser.parse( "field:energy" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 1 );

		luceneQuery = parser.parse( "component.componentProperty:noise" );
		helper.assertThat( luceneQuery ).matchesExactlyIds( 1 );
	}

	@Test
	public void testScopedAnalyzersFromSearchFactory() throws Exception {
		SearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MY_ENTITY_TYPE_ID );

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
			searchFactory.getAnalyzer( (IndexedTypeIdentifier) null );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		try {
			searchFactory.getAnalyzer( PojoIndexedTypeIdentifier.convertFromLegacy( null ) );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}

		try {
			searchFactory.getAnalyzer( PojoIndexedTypeIdentifier.convertFromLegacy( String.class ) );
		}
		catch (IllegalArgumentException iae) {
			log.debug( "success" );
		}
	}

	@Test
	public void testNotAnalyzedFieldAndScopedAnalyzer() throws Exception {
		SearchIntegrator searchFactory = sfHolder.getSearchFactory();
		Analyzer analyzer = searchFactory.getAnalyzer( MY_ENTITY_TYPE_ID );

		// you can pass what so ever into the analysis since the used analyzers are
		// returning the same tokens all the time. We just want to make sure that
		// the right analyzers are used.
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "notAnalyzed", "pass through" );
		assertTokensEqual( tokens, new String[] { "pass through" } );
	}

	public static void assertTokensEqual(Token[] tokens, String[] strings) {
		Assert.assertEquals( strings.length, tokens.length );

		for ( int i = 0; i < tokens.length; i++ ) {
			Assert.assertEquals( "index " + i, strings[i], AnalyzerUtils.getTermText( tokens[i] ) );
		}
	}

}
