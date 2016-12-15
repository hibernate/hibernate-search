/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hibernate.search.test.analyzer.AnalyzerTest.assertTokensEqual;
import static org.junit.Assert.assertEquals;

import java.util.Map;

/**
 * Test to verify HSEARCH-267.
 *
 * A base class defines a field as indexable without specifying an explicit analyzer. A subclass then defines an analyzer
 * at class level. This should also be the analyzer used for indexing the field in the base class.
 *
 * @author Hardy Ferentschik
 */
public class AnalyzerInheritanceTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	/**
	 * Try to verify that the right analyzer is used when indexing.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testBySearch() throws Exception {
		SubClass testClass = new SubClass();

		// See https://en.wikipedia.org/wiki/Dotted_and_dotless_I
		testClass.setName( "I\u0307stanbul" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( testClass );
		tx.commit();

		tx = s.beginTransaction();


		QueryParser parser = new QueryParser( "name", TestConstants.keywordAnalyzer );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "name:istanbul" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery, SubClass.class );
		assertEquals( 1, query.getResultSize() );

		// make sure the result is not always 1
		luceneQuery = parser.parse( "name:foo" );
		query = s.createFullTextQuery( luceneQuery, SubClass.class );
		assertEquals( 0, query.getResultSize() );

		tx.commit();
		s.close();
	}

	/**
	 * Try to verify that the right analyzer is used by explicitly retrieving the analyzer form the factory.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	@Category(SkipOnElasticsearch.class) // Analyzers cannot be retrieved directly when using Elasticsearch
	public void testByAnalyzerRetrieval() throws Exception {

		FullTextSession s = Search.getFullTextSession( openSession() );
		Analyzer analyzer = s.getSearchFactory().getAnalyzer( SubClass.class );

		// See https://en.wikipedia.org/wiki/Dotted_and_dotless_I
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", "I\u0307stanbul" );
		assertTokensEqual( tokens, new String[] { "istanbul" } );

		s.close();
	}

	@Override
	public void configure(Map<String, Object> settings) {
		super.configure( settings );
		settings.put( Environment.ANALYZER_CLASS, KeywordAnalyzer.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { SubClass.class };
	}
}
