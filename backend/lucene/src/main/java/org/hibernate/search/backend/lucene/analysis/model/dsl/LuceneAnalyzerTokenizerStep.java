/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.apache.lucene.analysis.TokenizerFactory;

/**
 * The step in an analyzer definition where the tokenizer can be set.
 */
public interface LuceneAnalyzerTokenizerStep {

	/**
	 * Set the tokenizer to use.
	 *
	 * @param factoryName The name of the factory that will create the tokenizer.
	 * The list of available names can be looked up with
	 * {@link org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext#availableTokenizers()}.
	 * @return The next step.
	 */
	LuceneAnalyzerOptionalComponentsStep tokenizer(String factoryName);

	/**
	 * Set the tokenizer to use.
	 *
	 * @param factoryType The type of the factory that will create the tokenizer.
	 * @return The next step.
	 */
	LuceneAnalyzerOptionalComponentsStep tokenizer(Class<? extends TokenizerFactory> factoryType);

}
