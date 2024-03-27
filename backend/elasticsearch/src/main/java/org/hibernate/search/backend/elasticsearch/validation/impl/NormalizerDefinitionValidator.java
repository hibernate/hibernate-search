/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

class NormalizerDefinitionValidator extends AnalysisDefinitionValidator<NormalizerDefinition> {

	NormalizerDefinitionValidator() {
		super(
				new AnalysisParameterEquivalenceRegistry.Builder()
						.build()
		);
	}

	@Override
	public void validate(ValidationErrorCollector errorCollector, NormalizerDefinition expectedDefinition,
			NormalizerDefinition actualDefinition) {
		super.validate( errorCollector, expectedDefinition, actualDefinition );

		if ( !Objects.equals( expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerCharFilters(
					expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) );
		}

		if ( !Objects.equals( expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerTokenFilters(
					expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) );
		}
	}

	@Override
	protected String getDefaultType() {
		return "custom";
	}
}
