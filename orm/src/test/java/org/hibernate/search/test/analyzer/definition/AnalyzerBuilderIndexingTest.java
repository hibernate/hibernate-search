/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.definition;

import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Tests that built analyzers are indeed used when indexing.
 */
@RunWith( Parameterized.class )
public class AnalyzerBuilderIndexingTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( Team.class )
			.withProperty( "hibernate.search.lucene_version", org.apache.lucene.util.Version.LATEST.toString() );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Parameters(name = "Analyzer {0}, terms {1}")
	public static Object[][] data() {
		return new Object[][] {
			{ "customanalyzer", "This is a D\u00E0scription", new String[] { "dascript" }, new String[] { "D\u00E0scription", "dascription", "is" } },
			{ "stemmer_override_analyzer", "This is a D\u00E0scription", new String[] { "dascription" }, new String[] { "D\u00E0scription", "dascript" } },
			{ "standard_analyzer", "This is just FOOBAR's", new String[] { "This", "is", "just", "FOOBAR's" }, new String[] { "FOOBAR" } },
			{ "html_standard_analyzer", "This is <b>foo</b><i>bar's</i>", new String[] { "This", "is", "foobar's" }, new String[] { "<b>", "b" } },
			{ "html_whitespace_analyzer", "This is <b>foo</b><i>bar's</i>", new String[] { "This", "is", "foobar's" }, new String[] { "<b>", "b" } },
			{ "trim_analyzer", " Kittens!   ", new String[] { "Kittens!" }, new String[] { " Kittens!   " } },
			{ "length_analyzer", "ab abc abcd abcde abcdef", new String[] { "abc", "abcd", "abcde" }, new String[] { "ab", "abcdef" } },
			{ "porter_analyzer", "bikes", new String[] { "bike" }, new String[] { "bikes" } },
			{ "porter_analyzer", "biking", new String[] { "bike" }, new String[] { "biking" } },
			{ "word_analyzer", "CamelCase", new String[] { "Camel", "Case" }, new String[] { "CamelCase" } },
			{ "synonym_analyzer", "ipod cosmos", new String[] { "ipod", "universe" }, new String[] { "cosmos" } },
			{ "shingle_analyzer", "please divide this sentence into shingles",
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
					},
				new String[] { "please divide this" }
			},
			{ "pattern_analyzer", "foo,bar", new String[] { "foo", "bar" }, new String[] { "foo,bar" } },
			{ "mapping_char_analyzer", "CORA\u00C7\u00C3O DE MEL\u00C3O", new String[] { "CORACAO", "DE", "MELAO" }, new String[] { "CORA\u00C7\u00C3O" } },
			{ "custom_normalizer", "This is a D\u00E0scription", new String[] { "this is a dascription" },
					new String[] { "This", "this", "is", "a", "D\u00E0scription", "dascription" } }
		};
	}

	private final String analyzerName;

	private final String stringToIndex;

	private final String[] expectedTokens;

	private final String[] unexpectedTokens;

	public AnalyzerBuilderIndexingTest(String analyzerName, String stringToIndex, String[] expectedTokens, String[] unexpectedTokens) {
		super();
		this.analyzerName = analyzerName;
		this.stringToIndex = stringToIndex;
		this.expectedTokens = expectedTokens;
		this.unexpectedTokens = unexpectedTokens;
	}

	@Test
	public void test() {
		Team team = new Team( 1 );
		team.setName( stringToIndex );

		helper.index( team );

		if ( unexpectedTokens != null ) {
			for ( String token : unexpectedTokens ) {
				String fieldName = "name_" + analyzerName;
				Query query = new TermQuery( new Term( fieldName, token ) );
				helper.assertThat( query )
						.as( "Results of searching '" + token + "' on field '" + fieldName + "'" )
						.matchesNone();
			}
		}
		for ( String token : expectedTokens ) {
			String fieldName = "name_" + analyzerName;
			Query query = new TermQuery( new Term( fieldName, token ) );
			helper.assertThat( query )
					.as( "Results of searching '" + token + "' on field '" + fieldName + "'" )
					.matchesExactlyIds( team.getId() );
		}
	}

}
