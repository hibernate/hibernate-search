/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;

/**
 * A context allowing to define named analysis-related elements in an Elasticsearch backend:
 * analyzers, tokenizers, char filters, ...
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchAnalysisDefinitionContainerContext {

	/**
	 * Start an analyzer definition.
	 * @param name The name used to reference this analyzer (both in Elasticsearch and in Hibernate Search).
	 * @return The analyzer definition context, allowing to define the analyzer's components (tokenizer, ...).
	 */
	ElasticsearchAnalyzerDefinitionContext analyzer(String name);

	/**
	 * Start a normalizer definition.
	 * @param name The name used to reference this normalizer (both in Elasticsearch and in Hibernate Search).
	 * @return The normalizer definition context, allowing to define the normalizer's components (token filters, ...).
	 */
	ElasticsearchNormalizerDefinitionContext normalizer(String name);

	/**
	 * Start a tokenizer definition.
	 * @param name The name used to reference this tokenizer
	 * {@link ElasticsearchAnalyzerDefinitionContext#withTokenizer(String) in analyzer definitions}.
	 * @return The tokenizer definition context, allowing to define the tokenizer's type and parameters.
	 */
	ElasticsearchAnalysisComponentDefinitionContext tokenizer(String name);

	/**
	 * Start a char filter definition.
	 * @param name The name used to reference this char filter
	 * {@link ElasticsearchCompositeAnalysisDefinitionContext#withCharFilters(String...) in analyzer or normalizer definitions}.
	 * @return The char filter definition context, allowing to define the char filter's type and parameters.
	 */
	ElasticsearchAnalysisComponentDefinitionContext charFilter(String name);

	/**
	 * Start a token filter definition.
	 * @param name The name used to reference this token filter
	 * {@link ElasticsearchCompositeAnalysisDefinitionContext#withTokenFilters(String...) in analyzer or normalizer definitions}.
	 * @return The token filter definition context, allowing to define the token filter's type and parameters.
	 */
	ElasticsearchAnalysisComponentDefinitionContext tokenFilter(String name);

}
