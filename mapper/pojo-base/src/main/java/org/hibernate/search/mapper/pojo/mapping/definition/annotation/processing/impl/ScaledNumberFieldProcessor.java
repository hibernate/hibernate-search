/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class ScaledNumberFieldProcessor extends AbstractNonFullTextFieldAnnotationProcessor<ScaledNumberField> {

	@Override
	PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			ScaledNumberField annotation, String fieldName) {
		PropertyMappingScaledNumberFieldOptionsStep fieldContext = mappingContext.scaledNumberField( fieldName );
		int decimalScale = annotation.decimalScale();
		if ( decimalScale != AnnotationDefaultValues.DEFAULT_DECIMAL_SCALE ) {
			fieldContext.decimalScale( decimalScale );
		}
		return fieldContext;
	}

	@Override
	String getName(ScaledNumberField annotation) {
		return annotation.name();
	}

	@Override
	Projectable getProjectable(ScaledNumberField annotation) {
		return annotation.projectable();
	}

	@Override
	Searchable getSearchable(ScaledNumberField annotation) {
		return annotation.searchable();
	}

	@Override
	Sortable getSortable(ScaledNumberField annotation) {
		return annotation.sortable();
	}

	@Override
	Aggregable getAggregable(ScaledNumberField annotation) {
		return annotation.aggregable();
	}

	@Override
	String getIndexNullAs(ScaledNumberField annotation) {
		return annotation.indexNullAs();
	}

	@Override
	ValueBridgeRef getValueBridge(ScaledNumberField annotation) {
		return annotation.valueBridge();
	}

	@Override
	ValueBinderRef getValueBinder(ScaledNumberField annotation) {
		return annotation.valueBinder();
	}

	@Override
	ContainerExtraction getExtraction(ScaledNumberField annotation) {
		return annotation.extraction();
	}
}
