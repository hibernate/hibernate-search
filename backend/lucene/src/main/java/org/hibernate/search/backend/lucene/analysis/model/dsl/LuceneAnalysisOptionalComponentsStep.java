/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;

/**
 * The step in an analyzer/normalizer definition
 * where optional components such as char filters or token filters can be added.
 */
public interface LuceneAnalysisOptionalComponentsStep {

	/**
	 * Add a char filter that the analyzer will use.
	 *
	 * @param factoryName The name of the factory that will create the char filter.
	 * The list of available names can be looked up with
	 * {@link org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext#availableCharFilters()}.
	 * @return The next step.
	 */
	LuceneAnalysisComponentParametersStep charFilter(String factoryName);

	/**
	 * Add a char filter that the analyzer will use.
	 *
	 * @param factoryType The type of the factory that will create the char filter.
	 * @return The next step.
	 */
	LuceneAnalysisComponentParametersStep charFilter(Class<? extends CharFilterFactory> factoryType);

	/**
	 * Add a token filter that the analyzer will use.
	 *
	 * @param factoryName The name of the factory that will create the token filter.
	 * The list of available names can be looked up with
	 * {@link org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext#availableTokenFilters()}.
	 * @return The next step.
	 */
	LuceneAnalysisComponentParametersStep tokenFilter(String factoryName);

	/**
	 * Add a token filter that the analyzer will use.
	 *
	 * @param factoryType The type of the factory that will create the token filter.
	 * @return The next step.
	 */
	LuceneAnalysisComponentParametersStep tokenFilter(Class<? extends TokenFilterFactory> factoryType);

}
