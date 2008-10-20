// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.search.test.analyzer.inheritance;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.queryParser.QueryParser;
import org.slf4j.Logger;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.AnalyzerUtils;
import org.hibernate.search.util.LoggerFactory;

/**
 * Test to verify HSEARCH-267.
 *
 * A base class defines a field as indexable without specifying an explicit analyzer. A subclass then defines ab analyzer
 * at class level. This should also be the analyzer used for indexing the field in the base class.
 *
 * @author Hardy Ferentschik
 */
public class AnalyzerInheritanceTest extends SearchTestCase {

	public static final Logger log = LoggerFactory.make();

	/**
	 * Try to verify that the right analyzer is used by indexing and searching.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testBySearch() throws Exception {
		SubClass testClass = new SubClass();
		testClass.setName( "Proca\u00EFne" );
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		s.persist( testClass );
		tx.commit();

		tx = s.beginTransaction();


		QueryParser parser = new QueryParser( "name", s.getSearchFactory().getAnalyzer( SubClass.class ) );
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
	public void testByAnalyzerRetrieval() throws Exception {

		FullTextSession s = Search.getFullTextSession( openSession() );
		Analyzer analyzer = s.getSearchFactory().getAnalyzer( SubClass.class );

		Token[] tokens = AnalyzerUtils.tokensFromAnalysis(analyzer, "name", "Proca\u00EFne");
		AnalyzerUtils.assertTokensEqual( tokens, new String[]{"Procaine"});

		s.close();
	}


	protected Class[] getMappings() {
		return new Class[] { SubClass.class };
	}
}
