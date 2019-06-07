/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.analysis;

import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;

/**
 * An object responsible for configuring analysis in an Elasticsearch backend,
 * providing analysis-related definitions that can be referenced from the mapping.
 * <p>
 * Users can select an analysis configurer through the
 * {@link ElasticsearchBackendSettings#ANALYSIS_CONFIGURER configuration properties}.
 */
public interface ElasticsearchAnalysisConfigurer {

	/**
	 * Configures analysis as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure analysis.
	 */
	void configure(ElasticsearchAnalysisDefinitionContainerContext context);

}
