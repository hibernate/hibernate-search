/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

class AnalyzerDefinitionValidator extends AnalysisDefinitionValidator<AnalyzerDefinition> {

	AnalyzerDefinitionValidator() {
		super(
				new AnalysisParameterEquivalenceRegistry.Builder()
						.type( "keep_types" )
						.param( "types" ).unorderedArray()
						.end()
						.build()
		);
	}

	@Override
	public void validate(ValidationErrorCollector errorCollector, AnalyzerDefinition expectedDefinition,
			AnalyzerDefinition actualDefinition) {
		super.validate( errorCollector, expectedDefinition, actualDefinition );

		if ( !Objects.equals( expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerCharFilters(
					expectedDefinition.getCharFilters(), actualDefinition.getCharFilters()
			) );
		}

		if ( !Objects.equals( expectedDefinition.getTokenizer(), actualDefinition.getTokenizer() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerTokenizer(
					expectedDefinition.getTokenizer(), actualDefinition.getTokenizer()
			) );
		}

		if ( !Objects.equals( expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerTokenFilters(
					expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters()
			) );
		}
	}

	@Override
	protected String getDefaultType() {
		return "custom";
	}
}
