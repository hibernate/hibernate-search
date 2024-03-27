/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class NonStandardFieldProcessor extends AbstractFieldAnnotationProcessor<NonStandardField> {

	@Override
	PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			NonStandardField annotation, String fieldName) {
		return mappingContext.nonStandardField( fieldName );
	}

	@Override
	String getName(NonStandardField annotation) {
		return annotation.name();
	}

	@Override
	ValueBridgeRef getValueBridge(NonStandardField annotation) {
		return null;
	}

	@Override
	ValueBinderRef getValueBinder(NonStandardField annotation) {
		return annotation.valueBinder();
	}

	@Override
	ContainerExtraction getExtraction(NonStandardField annotation) {
		return annotation.extraction();
	}
}
