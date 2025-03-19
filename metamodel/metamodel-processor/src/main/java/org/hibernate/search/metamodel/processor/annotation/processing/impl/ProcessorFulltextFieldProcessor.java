/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStandardFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorFulltextFieldProcessor extends AbstractProcessorStandardFieldAnnotationProcessor {
	@Override
	PropertyMappingStandardFieldOptionsStep<?> initStandardFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName) {
		var fieldContext = mappingContext.fullTextField( fieldName );

		// NOTE: we are skipping reading analyzers on purpose!
		// we won't have their configuration while running a processor, and since the analyzer does not influence the search capabilities
		// it's relatively safe to just use the default ones instead:
		//
		// String analyzer = getAnnotationValueAsString( annotation, "analyzer", AnalyzerNames.DEFAULT );
		// String searchAnalyzer = getAnnotationValueAsString( annotation, "searchAnalyzer", "" );
		// if ( !analyzer.isEmpty() ) {
		// 	fieldContext.analyzer( analyzer );
		// }
		// if ( !searchAnalyzer.isEmpty() ) {
		// 	fieldContext.searchAnalyzer( searchAnalyzer );
		// }

		Norms norms = getNorms( annotation );
		if ( !Norms.DEFAULT.equals( norms ) ) {
			fieldContext.norms( norms );
		}

		TermVector termVector = getTermVector( annotation );
		if ( !TermVector.DEFAULT.equals( termVector ) ) {
			fieldContext.termVector( termVector );
		}
		List<Highlightable> highlightable = getHighlightable( annotation );
		if ( !( highlightable.size() == 1 && Highlightable.DEFAULT.equals( highlightable.get( 0 ) ) ) ) {
			fieldContext.highlightable( highlightable );
		}

		return fieldContext;
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		StringIndexFieldTypeOptionsStep<?> optionsStep = bindingContext.typeFactory().asString()
				.analyzer( getAnnotationValueAsString( annotation, "analyzer", AnalyzerNames.DEFAULT ) )
				.projectable( getProjectable( annotation ) )
				.searchable( getSearchable( annotation ) )
				.norms( getNorms( annotation ) )
				.termVector( getTermVector( annotation ) );

		String searchAnalyzer = getAnnotationValueAsString( annotation, "searchAnalyzer", "" );
		if ( !searchAnalyzer.isEmpty() ) {
			optionsStep.searchAnalyzer( searchAnalyzer );
		}
		List<Highlightable> highlightable = getHighlightable( annotation );
		if ( !( highlightable.size() == 1 && Highlightable.DEFAULT.equals( highlightable.get( 0 ) ) ) ) {
			optionsStep.highlightable( highlightable );
		}
		return Optional.of( optionsStep );
	}

	protected Norms getNorms(AnnotationMirror annotation) {
		return Norms.valueOf( getAnnotationValueAsString( annotation, "norms", "DEFAULT" ) );
	}

	protected TermVector getTermVector(AnnotationMirror annotation) {
		return TermVector.valueOf( getAnnotationValueAsString( annotation, "termVector", "DEFAULT" ) );
	}

	protected List<Highlightable> getHighlightable(AnnotationMirror annotation) {
		AnnotationValue value = getAnnotationValue( annotation, "highlightable" );
		if ( value != null && value.getValue() instanceof List<?> list ) {
			return list.stream().map( v -> Objects.toString( ( (AnnotationValue) v ).getValue(), null ) )
					.map( Highlightable::valueOf )
					.collect( Collectors.toList() );
		}
		return List.of( Highlightable.DEFAULT );
	}
}
