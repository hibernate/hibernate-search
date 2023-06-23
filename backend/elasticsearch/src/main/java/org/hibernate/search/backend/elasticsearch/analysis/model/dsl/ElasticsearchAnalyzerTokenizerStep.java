/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;

/**
 * The step in an analyzer definition where the tokenizer can be set.
 */
public interface ElasticsearchAnalyzerTokenizerStep {

	/**
	 * Set the tokenizer that the analyzer will use.
	 *
	 * @param name The name of the tokenizer.
	 * There must be a corresponding tokenizer definition on the Elasticsearch server.
	 * This can be achieved by defining the tokenizer
	 * {@link ElasticsearchAnalysisConfigurationContext#tokenizer(String) from Hibernate Search},
	 * by configuring the Elasticsearch server directly, or by using built-in tokenizers.
	 * @return The next step.
	 */
	ElasticsearchAnalyzerOptionalComponentsStep tokenizer(String name);

}
