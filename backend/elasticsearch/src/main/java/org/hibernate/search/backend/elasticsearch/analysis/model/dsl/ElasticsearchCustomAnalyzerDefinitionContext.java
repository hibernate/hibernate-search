/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


public interface ElasticsearchCustomAnalyzerDefinitionContext {

	/**
	 * Set the tokenizer that the analyzer will use.
	 *
	 * @param name The name of the tokenizer.
	 * There must be a corresponding tokenizer definition on the Elasticsearch server.
	 * This can be achieved by defining the tokenizer
	 * {@link ElasticsearchAnalysisDefinitionContainerContext#tokenizer(String) from Hibernate Search},
	 * by configuring the Elasticsearch server directly, or by using built-in tokenizers.
	 * @return A definition context allowing to define the analyzer's filters.
	 */
	ElasticsearchAnalyzerDefinitionWithTokenizerContext withTokenizer(String name);

}
