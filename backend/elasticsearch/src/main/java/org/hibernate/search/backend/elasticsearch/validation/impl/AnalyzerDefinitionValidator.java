/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.analysis.model.esnative.impl.AnalyzerDefinition;

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
	public void validate(ValidationErrorCollector errorCollector, AnalyzerDefinition expectedDefinition, AnalyzerDefinition actualDefinition) {
		super.validate( errorCollector, expectedDefinition, actualDefinition );

		if ( ! Objects.equals( expectedDefinition.getCharFilters(), actualDefinition.getCharFilters() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerCharFilters(
					expectedDefinition.getCharFilters(), actualDefinition.getCharFilters()
			) );
		}

		if ( ! Objects.equals( expectedDefinition.getTokenizer(), actualDefinition.getTokenizer() ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalyzerTokenizer(
					expectedDefinition.getTokenizer(), actualDefinition.getTokenizer()
			) );
		}

		if ( ! Objects.equals( expectedDefinition.getTokenFilters(), actualDefinition.getTokenFilters() ) ) {
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
