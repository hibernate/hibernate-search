/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

import com.google.gson.JsonElement;

class DynamicTemplateValidator implements Validator<DynamicTemplate> {

	private final Validator<JsonElement> extraAttributeValidator = new JsonElementValidator( new JsonElementEquivalence() );
	private final Validator<PropertyMapping> propertyMappingValidator = new PropertyMappingValidator();

	@Override
	public void validate(ValidationErrorCollector errorCollector, DynamicTemplate expected, DynamicTemplate actual) {
		if ( expected == null ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.dynamicTemplateUnexpected() );
			return;
		}
		if ( actual == null ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.dynamicTemplateMissing() );
			return;
		}

		LeafValidators.EQUAL.validate(
				errorCollector, ValidationContextType.DYNAMIC_TEMPLATE_ATTRIBUTE, "match_mapping_type",
				expected.getMatchMappingType(), actual.getMatchMappingType()
		);

		LeafValidators.EQUAL.validate(
				errorCollector, ValidationContextType.DYNAMIC_TEMPLATE_ATTRIBUTE, "path_match",
				expected.getPathMatch(), actual.getPathMatch()
		);

		extraAttributeValidator.validateAllIncludingUnexpected(
				errorCollector, ValidationContextType.DYNAMIC_TEMPLATE_ATTRIBUTE,
				expected.getExtraAttributes(), actual.getExtraAttributes()
		);

		errorCollector.push( ValidationContextType.DYNAMIC_TEMPLATE_ATTRIBUTE, "mapping" );
		try {
			propertyMappingValidator.validate(
					errorCollector, expected.getMapping(), actual.getMapping()
			);
		}
		finally {
			errorCollector.pop();
		}
	}
}
