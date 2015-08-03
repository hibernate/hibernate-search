/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.solr;

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.util.AnalyzerUtils;
import org.junit.Test;

import static org.hibernate.search.test.analyzer.AnalyzerTest.assertTokensEqual;
import static org.junit.Assert.assertEquals;

/**
 * Tests the Solr analyzer creation framework.
 * This test might be affected by the version of the Analyzers being used. If it was to fail
 * after an upgrade of a Lucene or Solr version, make sure the new dependency still respects
 * the value Version.LUCENE_30, or update the test (older enum values are eventually deprecated
 * and not enforced anymore).
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SolrAnalyzerTest extends SearchTestBase {

	/**
	 * Tests that the token filters applied to <code>Team</code> are successfully created and used. Refer to
	 * <code>Team</code> to see the exact definitions.
	 *
	 * @throws Exception in case the test fails
	 */
	@Test
	public void testAnalyzerDef() throws Exception {
		// create the test instance
		Team team = new Team();
		team.setDescription( "This is a D\u00E0scription" ); // \u00E0 == � - ISOLatin1AccentFilterFactory should strip of diacritic
		team.setLocation( "Atlanta" );
		team.setName( "ATL team" );

		// persist and index the test object
		FullTextSession fts = Search.getFullTextSession( openSession() );
		Transaction tx = fts.beginTransaction();
		fts.persist( team );
		tx.commit();
		fts.clear();

		// execute several search to show that the right tokenizers were applies
		tx = fts.beginTransaction();
		TermQuery query = new TermQuery( new Term( "description", "D\u00E0scription" ) );
		assertEquals(
				"iso latin filter should work. � should be a now", 0, fts.createFullTextQuery( query ).list().size()
		);

		query = new TermQuery( new Term( "description", "is" ) );
		assertEquals(
				"stop word filter should work. is should be removed", 0, fts.createFullTextQuery( query ).list().size()
		);

		query = new TermQuery( new Term( "description", "dascript" ) );
		assertEquals(
				"snowball stemmer should work. 'dascription' should be stemmed to 'dascript'",
				1,
				fts.createFullTextQuery( query ).list().size()
		);

		// cleanup
		fts.delete( fts.createFullTextQuery( query ).list().get( 0 ) );
		tx.commit();
		fts.close();
	}

	/**
	 * Tests the analyzers defined on {@link Team}.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testAnalyzers() throws Exception {
		FullTextSession fts = Search.getFullTextSession( openSession() );

		Analyzer analyzer = fts.getSearchFactory().getAnalyzer( "standard_analyzer" );
		String text = "This is just FOOBAR's";
		Token[] tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "This", "is", "just", "FOOBAR's" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "html_standard_analyzer" );
		text = "This is <b>foo</b><i>bar's</i>";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "This", "is", "foobar's" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "html_whitespace_analyzer" );
		text = "This is <b>foo</b><i>bar's</i>";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "This", "is", "foobar's" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "trim_analyzer" );
		text = " Kittens!   ";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "kittens" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "length_analyzer" );
		text = "ab abc abcd abcde abcdef";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "abc", "abcd", "abcde" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "length_analyzer" );
		text = "ab abc abcd abcde abcdef";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "abc", "abcd", "abcde" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "porter_analyzer" );
		text = "bikes bikes biking";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "bike", "bike", "bike" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "word_analyzer" );
		text = "CamelCase";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "Camel", "Case" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "synonym_analyzer" );
		text = "ipod universe cosmos";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "ipod", "universe", "universe" } );

		analyzer = fts.getSearchFactory().getAnalyzer( "shingle_analyzer" );
		text = "please divide this sentence into shingles";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual(
				tokens,
				new String[] {
						"please",
						"please divide",
						"divide",
						"divide this",
						"this",
						"this sentence",
						"sentence",
						"sentence into",
						"into",
						"into shingles",
						"shingles"
				}
		);

		analyzer = fts.getSearchFactory().getAnalyzer( "phonetic_analyzer" );
		text = "The quick brown fox jumped over the lazy dogs";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		AnalyzerUtils.displayTokens( analyzer, "name", text );
		assertTokensEqual(
				tokens, new String[] { "0", "KK", "BRN", "FKS", "JMPT", "OFR", "0", "LS", "TKS" }
		);

		analyzer = fts.getSearchFactory().getAnalyzer( "pattern_analyzer" );
		text = "foo,bar";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "foo", "bar" } );

		// CharStreamFactories test
		analyzer = fts.getSearchFactory().getAnalyzer( "mapping_char_analyzer" );
		text = "CORA\u00C7\u00C3O DE MEL\u00C3O";
		tokens = AnalyzerUtils.tokensFromAnalysis( analyzer, "name", text );
		assertTokensEqual( tokens, new String[] { "CORACAO", "DE", "MELAO" } );

		fts.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Team.class
		};
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( "hibernate.search.lucene_version", org.apache.lucene.util.Version.LATEST.toString() );
	}
}
