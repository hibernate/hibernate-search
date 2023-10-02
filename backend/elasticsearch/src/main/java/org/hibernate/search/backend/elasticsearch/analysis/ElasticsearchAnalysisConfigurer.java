/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;

/**
 * An object responsible for configuring analysis in an Elasticsearch index,
 * providing analysis-related definitions that can be referenced from the mapping.
 * <p>
 * Users can select an analysis configurer through the
 * {@link ElasticsearchIndexSettings#ANALYSIS_CONFIGURER configuration properties}.
 */
public interface ElasticsearchAnalysisConfigurer {

	/**
	 * Configures analysis as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure analysis.
	 */
	void configure(ElasticsearchAnalysisConfigurationContext context);

}
