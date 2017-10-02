/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.AnalyzerUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

import static org.hibernate.search.test.analyzer.common.AnalyzerTest.assertTokensEqual;

/**
 * Test to verify HSEARCH-267.
 *
 * A base class defines a field as indexable without specifying an explicit analyzer. A subclass then defines an analyzer
 * at class level. This should also be the analyzer used for indexing the field in the base class.
 *
 * @author Hardy Ferentschik
 */
public class AnalyzerInheritanceTest {

	private static final IndexedTypeIdentifier SUB_CLASS_TYPE_ID = PojoIndexedTypeIdentifier.convertFromLegacy( SubClass.class );

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( SubClass.class )
			.withProperty( Environment.ANALYZER_CLASS, KeywordAnalyzer.class.getName() );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	/**
	 * Try to verify that the right analyzer is used when indexing.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testBySearch() throws Exception {
		SubClass testClass = new SubClass( 1 );

		// See https://en.wikipedia.org/wiki/Dotted_and_dotless_I
		testClass.setName( "I\u0307stanbul" );
		helper.index( testClass );

		QueryParser parser = new QueryParser( "name", TestConstants.keywordAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "name:istanbul" );
		helper.assertThat( luceneQuery )
				.from( SubClass.class )
				.matchesExactlyIds( 1 );

		// make sure the result is not always 1
		luceneQuery = parser.parse( "name:foo" );
		helper.assertThat( luceneQuery )
				.from( SubClass.class )
				.matchesNone();
	}

	/**
	 * Try to verify that the right analyzer is used by explicitly retrieving the analyzer form the factory.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	@Category(SkipOnElasticsearch.class) // Analyzers cannot be retrieved directly when using Elasticsearch
	public void testByAnalyzerRetrieval() throws Exception {
		Analyzer analyzer = sfHolder.getSearchFactory().getAnalyzer( SUB_CLASS_TYPE_ID );

		// See https://en.wikipedia.org/wiki/Dotted_and_dotless_I
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", "I\u0307stanbul" );
		assertTokensEqual( tokens, new String[] { "istanbul" } );
	}

}
