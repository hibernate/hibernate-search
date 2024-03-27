/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	public static final String ANALYZER_STANDARD = "standard";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP = "same_base_as_ngram";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_NGRAM_3 = "ngram";
	public static final String ANALYZER_STANDARD_STANDARD_LOWERCASE_STOP_STEMMER_ENGLISH = "stemmer";
	public static final String ANALYZER_STANDARD_HTML_STRIP_ESCAPED_LOWERCASE = "htmlStrip";

}
