/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport;

public final class AnalysisNames {

	private AnalysisNames() {
	}

	// XXX add analyzer/normalizer definitions using Search 6 APIs
	public static final String NORMALIZER_LOWERCASE = "lower";
	public static final String NORMALIZER_LOWERCASE_ASCIIFOLDING = "lower_asciifolding";
	public static final String ANALYZER_WHITESPACE = "whitespace";
	public static final String ANALYZER_WHITESPACE_LOWERCASE_ASCIIFOLDING = "whitespace_lower_asciifolding";
	public static final String ANALYZER_KEYWORD_LOWERCASE_ASCIIFOLDING = "keyword_lower_asciifolding";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP = "same_base_as_ngram";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_NGRAM_3 = "ngram";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_STEMMER_ENGLISH = "stemmer";
	public static final String ANALYZER_STANDARD_HTML_STRIP_ESCAPED_LOWERCASE = "htmlStrip";

}
