/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

import com.google.gson.JsonElement;

class AnalysisDefinitionValidator<T extends AnalysisDefinition> implements Validator<T> {

	private final AnalysisParameterEquivalenceRegistry equivalences;

	AnalysisDefinitionValidator(AnalysisParameterEquivalenceRegistry equivalences) {
		this.equivalences = equivalences;
	}

	@Override
	public void validate(ValidationErrorCollector errorCollector, T expectedDefinition, T actualDefinition) {
		String expectedType = expectedDefinition.getType();
		String actualType = actualDefinition.getType();
		Object defaultedExpectedType = expectedType == null ? getDefaultType() : expectedType;
		Object defaultedActualType = actualType == null ? getDefaultType() : actualType;
		if ( !Objects.equals( defaultedExpectedType, defaultedActualType ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.invalidAnalysisDefinitionType(
					expectedType, actualType
			) );
		}

		String typeName = expectedDefinition.getType();
		Validator<JsonElement> parameterValidator = new JsonElementValidator(
				name -> equivalences.get( typeName, name )
		);

		parameterValidator.validateAllIncludingUnexpected(
				errorCollector, ValidationContextType.ANALYSIS_DEFINITION_PARAMETER,
				expectedDefinition.getParameters(), actualDefinition.getParameters()
		);
	}

	protected String getDefaultType() {
		return null;
	}
}
