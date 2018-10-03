/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.apache.lucene.analysis.Analyzer;

public interface LuceneNormalizerDefinitionContext {

	/**
	 * Start a custom normalizer definition,
	 * assigning char filters and token filters to the definition.
	 *
	 * @return A context allowing to further define the analyzer.
	 */
	LuceneCustomNormalizerDefinitionContext custom();

	/**
	 * Assign the given normalizer instance to this normalizer definition.
	 *
	 * @param instance The normalizer instance.
	 * This instance is expected to never produce more than one token per stream.
	 * @return The parent context, for method chaining.
	 */
	LuceneAnalysisDefinitionContainerContext instance(Analyzer instance);

}
