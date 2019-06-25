/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


/**
 * The initial step in an analyzer definition, where the type of analyzer can be set.
 */
public interface ElasticsearchAnalyzerTypeStep {

	/**
	 * Start a custom analyzer definition,
	 * assigning a tokenizer, and optionally char filters and token filters to the definition.
	 *
	 * @return The next step.
	 */
	ElasticsearchAnalyzerTokenizerStep custom();

	/**
	 * Start a typed analyzer definition,
	 * assigning a type, and optionally parameters to the definition.
	 *
	 * @param type The name of the analyzer type to configure.
	 * @return The next step.
	 */
	ElasticsearchAnalysisComponentParametersStep type(String type);

}
