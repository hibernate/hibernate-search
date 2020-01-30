/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;

public class RootTypeMappingValidator extends AbstractTypeMappingValidator<RootTypeMapping> {
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
	}

	@Override
	protected Validator<PropertyMapping> getPropertyMappingValidator() {
		return propertyMappingValidator;
	}
}
