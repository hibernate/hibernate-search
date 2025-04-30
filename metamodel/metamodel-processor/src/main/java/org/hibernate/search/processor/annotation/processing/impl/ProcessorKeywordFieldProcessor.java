/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorKeywordFieldProcessor extends AbstractProcessorNonFullTextFieldAnnotationProcessor {
	@Override
	PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName) {
		PropertyMappingKeywordFieldOptionsStep fieldContext = mappingContext.keywordField( fieldName );

		// NOTE: we are skipping reading analyzers on purpose!
		// we won't have their configuration while running a processor, and since the analyzer does not influence the search capabilities
		// it's relatively safe to just use the default ones instead:
		//
		// String normalizer = getAnnotationValueAsString( annotation, "normalizer", "" );
		// if ( !normalizer.isEmpty() ) {
		// 	fieldContext.normalizer( normalizer );
		// }

		Norms norms = getNorms( annotation );
		if ( !Norms.DEFAULT.equals( norms ) ) {
			fieldContext.norms( norms );
		}

		return fieldContext;
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		StringIndexFieldTypeOptionsStep<?> optionsStep = bindingContext.typeFactory().asString()
				.projectable( getProjectable( annotation ) )
				.sortable( getSortable( annotation ) )
				.searchable( getSearchable( annotation ) )
				.aggregable( getAggregable( annotation ) )
				.norms( getNorms( annotation ) )
				.indexNullAs( getIndexNullAs( annotation ) );

		String normalizer = getAnnotationValueAsString( annotation, "normalizer", "" );
		if ( !normalizer.isEmpty() ) {
			optionsStep.normalizer( normalizer );
		}
		return Optional.of( optionsStep );
	}

	protected Norms getNorms(AnnotationMirror annotation) {
		return Norms.valueOf( getAnnotationValueAsString( annotation, "norms", "DEFAULT" ) );
	}
}
