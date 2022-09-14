/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

public class RootTypeMappingValidator extends AbstractTypeMappingValidator<RootTypeMapping> {
	private final Validator<List<NamedDynamicTemplate>> dynamicTemplatesValidator = new NamedDynamicTemplateListValidator();
	private final Validator<PropertyMapping> propertyMappingValidator = new PropertyMappingValidator();

	@Override
	public void validate(ValidationErrorCollector errorCollector,
			RootTypeMapping expectedMapping, RootTypeMapping actualMapping) {
		if ( expectedMapping == null ) {
			return;
		}
		if ( actualMapping == null ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.mappingMissing() );
			return;
		}

		super.validate( errorCollector, expectedMapping, actualMapping );

		dynamicTemplatesValidator.validate( errorCollector, expectedMapping.getDynamicTemplates(),
				actualMapping.getDynamicTemplates() );
	}

	@Override
	protected Validator<PropertyMapping> getPropertyMappingValidator() {
		return propertyMappingValidator;
	}
}
