/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisComponentTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisOptionalComponentsStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTokenizerStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalyzerTypeStep;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchNormalizerTypeStep;

/**
 * A context allowing the definition of named analysis-related elements in an Elasticsearch backend:
 * analyzers, tokenizers, char filters, ...
 */
public interface ElasticsearchAnalysisConfigurationContext {

	/**
	 * Start an analyzer definition.
	 * @param name The name used to reference this analyzer (both in Elasticsearch and in Hibernate Search).
	 * @return The initial step of a DSL where the analyzer can be defined.
	 */
	ElasticsearchAnalyzerTypeStep analyzer(String name);

	/**
	 * Start a normalizer definition.
	 * @param name The name used to reference this normalizer (both in Elasticsearch and in Hibernate Search).
	 * @return The initial step of a DSL where the normalizer can be defined.
	 */
	ElasticsearchNormalizerTypeStep normalizer(String name);

	/**
	 * Start a tokenizer definition.
	 * @param name The name used to reference this tokenizer
	 * {@link ElasticsearchAnalyzerTokenizerStep#tokenizer(String) in analyzer definitions}.
	 * @return The initial step of a DSL where the tokenizer can be defined.
	 */
	ElasticsearchAnalysisComponentTypeStep tokenizer(String name);

	/**
	 * Start a char filter definition.
	 * @param name The name used to reference this char filter
	 * {@link ElasticsearchAnalysisOptionalComponentsStep#charFilters(String...) in analyzer or normalizer definitions}.
	 * @return The initial step of a DSL where the char filter can be defined.
	 */
	ElasticsearchAnalysisComponentTypeStep charFilter(String name);

	/**
	 * Start a token filter definition.
	 * @param name The name used to reference this token filter
	 * {@link ElasticsearchAnalysisOptionalComponentsStep#tokenFilters(String...) in analyzer or normalizer definitions}.
	 * @return The initial step of a DSL where the token filter can be defined.
	 */
	ElasticsearchAnalysisComponentTypeStep tokenFilter(String name);

}
