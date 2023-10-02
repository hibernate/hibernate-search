/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.analysis.model.dsl;


/**
 * The initial step in an analyzer definition, where the type of normalizer can be set.
 */
public interface ElasticsearchNormalizerTypeStep {

	/**
	 * Start a custom normalizer definition,
	 * assigning char filters and token filters to the definition.
	 *
	 * @return The next step.
	 */
	ElasticsearchNormalizerOptionalComponentsStep custom();

}
