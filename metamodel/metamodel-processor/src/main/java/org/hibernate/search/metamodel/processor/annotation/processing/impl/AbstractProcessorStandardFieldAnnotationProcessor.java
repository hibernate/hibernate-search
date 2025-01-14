/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import javax.lang.model.element.AnnotationMirror;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public abstract class AbstractProcessorStandardFieldAnnotationProcessor extends AbstractProcessorFieldAnnotationProcessor {
	@Override
	PropertyMappingStandardFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation,
			String fieldName) {
		PropertyMappingStandardFieldOptionsStep<?> fieldContext = initStandardFieldMappingContext(
				mappingContext, annotation, fieldName );

		Projectable projectable = getProjectable( annotation );
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			fieldContext.projectable( projectable );
		}

		Searchable searchable = getSearchable( annotation );
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			fieldContext.searchable( searchable );
		}

		return fieldContext;
	}

	protected Searchable getSearchable(AnnotationMirror annotation) {
		return Searchable.valueOf( getAnnotationValueAsString( annotation, "searchable", "DEFAULT" ) );
	}

	protected Projectable getProjectable(AnnotationMirror annotation) {
		return Projectable.valueOf( getAnnotationValueAsString( annotation, "projectable", "DEFAULT" ) );
	}

	abstract PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(
			PropertyMappingStep mappingContext, AnnotationMirror annotation, String fieldName);
}
