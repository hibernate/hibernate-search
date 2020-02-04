/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;

abstract class AbstractTypeMappingValidator<T extends AbstractTypeMapping> implements Validator<T> {

	protected abstract Validator<PropertyMapping> getPropertyMappingValidator();

	@Override
	public void validate(ValidationErrorCollector errorCollector, T expectedMapping, T actualMapping) {
		DynamicType expectedDynamic = expectedMapping.getDynamic();
		if ( expectedDynamic != null ) { // If not provided, we don't care
			LeafValidators.EQUAL.validateWithDefault(
					errorCollector, ValidationContextType.MAPPING_ATTRIBUTE, "dynamic",
					expectedDynamic, actualMapping.getDynamic(), DynamicType.TRUE
			);
		}

		getPropertyMappingValidator().validateAllIgnoreUnexpected(
				errorCollector, ValidationContextType.MAPPING_PROPERTY,
				ElasticsearchValidationMessages.INSTANCE.propertyMissing(),
				expectedMapping.getProperties(), actualMapping.getProperties()
		);
	}
}
