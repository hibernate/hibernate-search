/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import javax.lang.model.element.AnnotationMirror;

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

abstract class AbstractProcessorNonFullTextFieldAnnotationProcessor
		extends AbstractProcessorStandardFieldAnnotationProcessor {
	@Override
	PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName) {
		PropertyMappingNonFullTextFieldOptionsStep<?> fieldContext = initSortableFieldMappingContext(
				mappingContext, annotation, fieldName
		);

		Sortable sortable = getSortable( annotation );
		if ( !Sortable.DEFAULT.equals( sortable ) ) {
			fieldContext.sortable( sortable );
		}

		Aggregable aggregable = getAggregable( annotation );
		if ( !Aggregable.DEFAULT.equals( aggregable ) ) {
			fieldContext.aggregable( aggregable );
		}

		String indexNullAs = getIndexNullAs( annotation );
		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			fieldContext.indexNullAs( indexNullAs );
		}

		return fieldContext;
	}

	abstract PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(
			PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName);

	protected Sortable getSortable(AnnotationMirror annotation) {
		return Sortable.valueOf( getAnnotationValueAsString( annotation, "sortable", "DEFAULT" ) );
	}

	protected Aggregable getAggregable(AnnotationMirror annotation) {
		return Aggregable.valueOf( getAnnotationValueAsString( annotation, "aggregable", "DEFAULT" ) );
	}

	protected String getIndexNullAs(AnnotationMirror annotation) {
		return getAnnotationValueAsString( annotation, "indexNullAs", AnnotationDefaultValues.DO_NOT_INDEX_NULL );
	}

}
