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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public class GenericFieldProcessor extends AbstractNonFullTextFieldAnnotationProcessor<GenericField> {

	@Override
	PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			GenericField annotation, String fieldName) {
		return mappingContext.genericField( fieldName );
	}

	@Override
	String getName(GenericField annotation) {
		return annotation.name();
	}

	@Override
	Projectable getProjectable(GenericField annotation) {
		return annotation.projectable();
	}

	@Override
	Searchable getSearchable(GenericField annotation) {
		return annotation.searchable();
	}

	@Override
	Sortable getSortable(GenericField annotation) {
		return annotation.sortable();
	}

	@Override
	Aggregable getAggregable(GenericField annotation) {
		return annotation.aggregable();
	}

	@Override
	String getIndexNullAs(GenericField annotation) {
		return annotation.indexNullAs();
	}

	@Override
	ValueBridgeRef getValueBridge(GenericField annotation) {
		return annotation.valueBridge();
	}

	@Override
	ValueBinderRef getValueBinder(GenericField annotation) {
		return annotation.valueBinder();
	}

	@Override
	ContainerExtraction getExtraction(GenericField annotation) {
		return annotation.extraction();
	}
}
