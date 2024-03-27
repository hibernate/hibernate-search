/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchValidationMessages;

public class NamedDynamicTemplateListValidator implements Validator<List<NamedDynamicTemplate>> {

	private final DynamicTemplateValidator dynamicTemplateValidator;

	public NamedDynamicTemplateListValidator(Validator<PropertyMapping> propertyMappingValidator) {
		this.dynamicTemplateValidator = new DynamicTemplateValidator( propertyMappingValidator );
	}

	@Override
	public void validate(ValidationErrorCollector errorCollector, List<NamedDynamicTemplate> expected,
			List<NamedDynamicTemplate> actual) {
		if ( expected == null ) {
			expected = Collections.emptyList();
		}
		if ( actual == null ) {
			actual = Collections.emptyList();
		}

		// First, check existence and compare content
		Map<String, DynamicTemplate> expectedAsMap = toMapReportingDuplicates( errorCollector, expected );
		Map<String, DynamicTemplate> actualAsMap = toMapReportingDuplicates( errorCollector, actual );
		dynamicTemplateValidator.validateAllIncludingUnexpected(
				errorCollector, ValidationContextType.DYNAMIC_TEMPLATE,
				expectedAsMap, actualAsMap
		);

		// Finally, check order
		List<String> expectedNamesInOrder = expected.stream().map( NamedDynamicTemplate::getName )
				.collect( Collectors.toList() );
		List<String> actualNamesInOrder = actual.stream().map( NamedDynamicTemplate::getName )
				.collect( Collectors.toList() );
		// Only consider names that we actually expected
		actualNamesInOrder.retainAll( expectedNamesInOrder );
		// Skip the check if some templates are missing or duplicated;
		// in this case other, more relevant errors have already been raised above.
		if ( actualNamesInOrder.containsAll( expectedNamesInOrder )
				&& actualNamesInOrder.size() == expectedNamesInOrder.size()
				&& !actualNamesInOrder.equals( expectedNamesInOrder ) ) {
			errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.dynamicTemplatesInvalidOrder(
					expectedNamesInOrder, actualNamesInOrder
			) );
		}
	}

	private Map<String, DynamicTemplate> toMapReportingDuplicates(ValidationErrorCollector errorCollector,
			List<NamedDynamicTemplate> list) {
		Map<String, DynamicTemplate> result = new LinkedHashMap<>();
		for ( NamedDynamicTemplate template : list ) {
			DynamicTemplate previous = result.putIfAbsent( template.getName(), template.getTemplate() );
			if ( previous != null ) {
				errorCollector.push( ValidationContextType.DYNAMIC_TEMPLATE, template.getName() );
				try {
					errorCollector.addError( ElasticsearchValidationMessages.INSTANCE.dynamicTemplateDuplicate() );
				}
				finally {
					errorCollector.pop();
				}
			}
		}
		return result;
	}

}
