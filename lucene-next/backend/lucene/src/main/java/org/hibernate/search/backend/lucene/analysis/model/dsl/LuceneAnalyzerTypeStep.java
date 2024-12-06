/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;

import org.apache.lucene.analysis.Analyzer;

/**
 * The initial step in an analyzer definition, where the type of analyzer can be set.
 */
public interface LuceneAnalyzerTypeStep {

	/**
	 * Start a custom analyzer definition,
	 * assigning a tokenizer, and optionally char filters and token filters to the definition.
	 *
	 * @return The next step.
	 */
	LuceneAnalyzerTokenizerStep custom();

	/**
	 * Assign the given analyzer instance to this analyzer definition.
	 *
	 * @param instance The analyzer instance.
	 * @return The parent context, for method chaining.
	 */
	LuceneAnalysisConfigurationContext instance(Analyzer instance);

}
