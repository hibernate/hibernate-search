/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

public interface LuceneAnalysisDefinitionContainerContext {

	/**
	 * Start a new analyzer definition.
	 *
	 * @param name The name used to reference this analyzer in Hibernate Search.
	 * @return A context allowing to further define the analyzer.
	 */
	LuceneAnalyzerDefinitionContext analyzer(String name);

	/**
	 * Start a new normalizer definition.
	 *
	 * @param name The name used to reference this normalizer in Hibernate Search.
	 * @return A context allowing to further define the normalizer.
	 */
	LuceneNormalizerDefinitionContext normalizer(String name);

}
