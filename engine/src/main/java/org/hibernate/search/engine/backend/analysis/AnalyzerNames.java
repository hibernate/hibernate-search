/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * Unless overridden by explicit analysis configuration,
	 * the default analyzer will be the {@link #STANDARD} analyzer.
	 */
	public static final String DEFAULT = "default";

	/**
	 * The standard analyzer.
	 * <ul>
	 *     <li>First, tokenize using the standard tokenizer, which follows Word Break rules from the
	 *     Unicode Text Segmentation algorithm, as specified in
	 *     <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.</li>
	 *     <li>Then, it removes most punctuation.</li>
	 *     <li>Finally, lowercase each token.</li>
	 * </ul>
	 */
	public static final String STANDARD = "standard";

	/**
	 * The simple analyzer.
	 * <ul>
	 *     <li>First, tokenize the text into tokens whenever it encounters a character which is not a letter.</li>
	 *     <li>Then, lowercase each token.</li>
	 * </ul>
	 */
	public static final String SIMPLE = "simple";

	/**
	 * The whitespace analyzer.
	 * <ul>
	 *     <li>Tokenize the text into tokens whenever it encounters a character which is not a white space.</li>
	 *     <li>Do not change the tokens.</li>
	 * </ul>
	 */
	public static final String WHITESPACE = "whitespace";

	/**
	 * The stop analyzer.
	 * <ul>
	 *     <li>First, tokenize the text into tokens whenever it encounters a character which is not a letter.</li>
	 *     <li>Then, it removes english stop words.</li>
	 *     <li>Finally, lowercase each token.</li>
	 * </ul>
	 */
	public static final String STOP = "stop";

	/**
	 * The keyword analyzer.
	 * <p>
	 * Do not change in any way the text.
	 * With this analyzer a full text field would behave exactly as it was a keyword field.
	 * <p>
	 * Maybe you should consider using a keyword field instead.
	 */
	public static final String KEYWORD = "keyword";

}
