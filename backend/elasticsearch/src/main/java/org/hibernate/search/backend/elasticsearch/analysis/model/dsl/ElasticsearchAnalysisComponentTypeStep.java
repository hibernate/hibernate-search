/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


/**
 * The step in an analysis component definition where the type of that component can be set.
 */
public interface ElasticsearchAnalysisComponentTypeStep {

	/**
	 * Set the Elasticsearch type to use for the definition.
	 *
	 * @param name The value for the 'type' parameter in the Elasticsearch definition.
	 * @return The next step.
	 */
	ElasticsearchAnalysisComponentParametersStep type(String name);

}
