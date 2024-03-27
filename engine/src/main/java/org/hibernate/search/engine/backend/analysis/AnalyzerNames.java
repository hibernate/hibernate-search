/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.analysis;

/**
 * Constants for the names of built-in analyzers.
 */
public final class AnalyzerNames {

	private AnalyzerNames() {
	}

	/**
	 * The default analyzer.
	 * <p>
	 * This analyzer will generally be used for full-text field that don't require specific analysis.
	 * <p>
	 * Unless overridden by explicit analysis configuration, the default analyzer will be the standard analyzer:
	 * <ul>
	 *     <li>First, tokenize using the standard tokenizer, which follows Word Break rules from the
	 *     Unicode Text Segmentation algorithm, as specified in
	 *     <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.</li>
	 *     <li>Then, lowercase each token.</li>
	 * </ul>
	 */
	public static final String DEFAULT = "default";

	/**
	 * The standard analyzer.
	 * <p>
	 * Unless overridden by explicit analysis configuration, this analyzer behaves as follows:
	 * <ul>
	 *     <li>First, tokenize using the standard tokenizer, which follows Word Break rules from the
	 *     Unicode Text Segmentation algorithm, as specified in
	 *     <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.</li>
	 *     <li>Then, lowercase each token.</li>
	 * </ul>
	 */
	public static final String STANDARD = "standard";

	/**
	 * The simple analyzer.
	 * <p>
	 * Unless overridden by explicit analysis configuration, this analyzer behaves as follows:
	 * <ul>
	 *     <li>First, tokenize by assuming non-letter characters are separators.</li>
	 *     <li>Then, lowercase each token.</li>
	 * </ul>
	 */
	public static final String SIMPLE = "simple";

	/**
	 * The whitespace analyzer.
	 * <p>
	 * Unless overridden by explicit analysis configuration, this analyzer behaves as follows:
	 * <ul>
	 *     <li>First, tokenize by assuming whitespace characters are separators.</li>
	 *     <li>Do not change the tokens.</li>
	 * </ul>
	 */
	public static final String WHITESPACE = "whitespace";

	/**
	 * The stop analyzer.
	 * <p>
	 * Unless overridden by explicit analysis configuration, this analyzer behaves as follows:
	 * <ul>
	 *     <li>First, tokenize by assuming non-letter characters are separators.</li>
	 *     <li>Then, lowercase each token.</li>
	 *     <li>finally, remove english stop words.</li>
	 * </ul>
	 */
	public static final String STOP = "stop";

	/**
	 * The keyword analyzer.
	 * <p>
	 * Unless overridden by explicit analysis configuration, this analyzer does not change the text in any way.
	 * <p>
	 * With this analyzer, a full text field would behave similarly to a keyword field,
	 * but with fewer features: no terms aggregations, for example.
	 * <p>
	 * Consider using a keyword field instead.
	 */
	public static final String KEYWORD = "keyword";

}
