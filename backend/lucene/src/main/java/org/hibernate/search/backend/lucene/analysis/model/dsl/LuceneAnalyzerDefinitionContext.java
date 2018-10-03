/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneAnalyzerDefinitionContext {

	/**
	 * Start a custom analyzer definition,
	 * assigning a tokenizer, and optionally char filters and token filters to the definition.
	 *
	 * @return A context allowing to further define the analyzer.
	 */
	LuceneCustomAnalyzerDefinitionContext custom();

	/**
	 * Assign the given analyzer instance to this analyzer definition.
	 *
	 * @param instance The analyzer instance.
	 * @return The parent context, for method chaining.
	 */
	LuceneAnalysisDefinitionContainerContext instance(Analyzer instance);

}
