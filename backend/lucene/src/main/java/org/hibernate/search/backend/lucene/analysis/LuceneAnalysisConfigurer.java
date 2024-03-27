/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis;

import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;

/**
 * A provider of analysis-related definitions that can be referenced from the mapping,
 * e.g. with {@literal @Analyzer(definition = "some-name")}
 * or {@literal @Normalizer(definition = "some-other-name")}.
 * <p>
 * Users can select a definition provider through the
 * {@link LuceneBackendSettings#ANALYSIS_CONFIGURER configuration properties}.
 */
public interface LuceneAnalysisConfigurer {

	/**
	 * Configures analysis as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure analysis.
	 */
	void configure(LuceneAnalysisConfigurationContext context);

}
