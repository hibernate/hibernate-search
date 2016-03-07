/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.analyzer.spi;

import org.apache.lucene.analysis.Analyzer;

/**
 * Applies to index managers that are bound to backends supporting built-in analyzer definitions.
 * <p>
 * For example, Elasticsearch comes with whitespace and languages analyzers without definition defined in
 * the classpath.
 *
 * @author Davide D'Alto
 */
public interface AnalyzerDefiner {

	/**
	 * Checks if the index manager can managed additional analyzer
	 *
	 * @return true if it supports additional analyzers without the need of an analyzer defintion
	 */
	boolean supportsAdditionalAnalyzer();

	/**
	 * Creates an analyzer for the provided definition.
	 *
	 * @param definitionName the name of the analyzer deifinition
	 * @return an {@link Analyzer} instance the can be used
	 */
	Analyzer createAnalyzer(String definitionName);
}
