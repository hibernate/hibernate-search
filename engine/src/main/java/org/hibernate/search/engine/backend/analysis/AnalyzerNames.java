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
	 * Unless overridden by explicit analysis configuration, the default analyzer will be the standard analyzer:
	 * <ul>
	 *     <li>First, tokenize using the standard tokenizer, which follows Word Break rules from the
	 *     Unicode Text Segmentation algorithm, as specified in
	 *     <a href="http://unicode.org/reports/tr29/">Unicode Standard Annex #29</a>.</li>
	 *     <li>Then, lowercase each token.</li>
	 * </ul>
	 */
	public static final String DEFAULT = "default";

}
