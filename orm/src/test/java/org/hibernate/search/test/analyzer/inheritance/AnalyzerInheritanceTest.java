/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.inheritance;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryparser.classic.QueryParser;

import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.AnalyzerUtils;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;

import static org.hibernate.search.test.analyzer.AnalyzerTest.assertTokensEqual;
import static org.junit.Assert.assertEquals;

/**
 * Test to verify HSEARCH-267.
 *
 * A base class defines a field as indexable without specifying an explicit analyzer. A subclass then defines ab analyzer
 * at class level. This should also be the analyzer used for indexing the field in the base class.
 *
 * @author Hardy Ferentschik
 */
public class AnalyzerInheritanceTest extends SearchTestBase {

	public static final Log log = LoggerFactory.make();

	/**
	 * Try to verify that the right analyzer is used by indexing and searching.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testBySearch() throws Exception {
		SubClass testClass = new SubClass();
		testClass.setName( "Proca\u00EFne" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( testClass );
		tx.commit();

		tx = s.beginTransaction();


		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", s.getSearchFactory().getAnalyzer( SubClass.class ) );
		org.apache.lucene.search.Query luceneQuery = parser.parse( "name:Proca\u00EFne" );
		FullTextQuery query = s.createFullTextQuery( luceneQuery, SubClass.class );
		assertEquals( 1, query.getResultSize() );

		luceneQuery = parser.parse( "name:Procaine" );
		query = s.createFullTextQuery( luceneQuery, SubClass.class );
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
	public void testByAnalyzerRetrieval() throws Exception {

		FullTextSession s = Search.getFullTextSession( openSession() );
		Analyzer analyzer = s.getSearchFactory().getAnalyzer( SubClass.class );

		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", "Proca\u00EFne" );
		assertTokensEqual( tokens, new String[] { "Procaine" } );

		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SubClass.class };
	}
}
