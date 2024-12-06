/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.analysis.model.dsl;

/**
 * The step in an analysis component definition where optional parameters can be set.
 */
public interface LuceneAnalysisComponentParametersStep extends LuceneAnalysisOptionalComponentsStep {

	/**
	 * Set a parameter.
	 *
	 * @param name The name of the parameter.
	 * @param value The value of the parameter.
	 * @return {@code this}, for method chaining.
	 */
	LuceneAnalysisComponentParametersStep param(String name, String value);

}
