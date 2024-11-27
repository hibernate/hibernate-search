/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;

import org.apache.lucene.analysis.Analyzer;

/**
 * The initial step in an analyzer definition, where the type of normalizer can be set.
 */
public interface LuceneNormalizerTypeStep {

	/**
	 * Start a custom normalizer definition,
	 * assigning char filters and token filters to the definition.
	 *
	 * @return The next step.
	 */
	LuceneNormalizerOptionalComponentsStep custom();

	/**
	 * Assign the given normalizer instance to this normalizer definition.
	 *
	 * @param instance The normalizer instance.
	 * This instance is expected to never produce more than one token per stream.
	 * @return The next step.
	 */
	LuceneAnalysisConfigurationContext instance(Analyzer instance);

}
