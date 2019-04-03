/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.configuration;

/**
 * The analysis definitions that are expected to be present in the predicate tests in which analysis overriding is tested
 * <p>
 * Access {@link #name} to get the expected name .
 * See the javadoc for a description of what is expected in each definition.
 */
public enum OverrideAnalysisDefinitions {
	/**
	 * Analyzer with a tokenizer on whitespaces.
	 */
	ANALYZER_WHITESPACE("analyzer_whitespace"),

	/**
	 * Analyzer with a tokenizer on whitespaces and a lowercase token filter.
	 */
	ANALYZER_WHITESPACE_LOWERCASE("analyzer_whitespace_lowercase"),

	/**
	 * Ngram analyzer defined for query only.
	 */
	ANALYZER_NGRAM("analyzer_ngram");

	public final String name;

	OverrideAnalysisDefinitions(String suffix) {
		this.name = getClass().getSimpleName() + "_" + suffix;
	}
}
