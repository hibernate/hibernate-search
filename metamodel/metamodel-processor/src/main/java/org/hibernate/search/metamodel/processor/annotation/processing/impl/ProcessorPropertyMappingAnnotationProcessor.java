/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public interface ProcessorPropertyMappingAnnotationProcessor {
	static boolean documentId(AnnotationMirror annotation) {
		return getQualifiedName( annotation )
				.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId" );
	}

	static boolean ormId(AnnotationMirror annotation) {
		return getQualifiedName( annotation ).contentEquals( "jakarta.persistence.Id" );
	}

	static Optional<ProcessorPropertyMappingAnnotationProcessor> processor(AnnotationMirror annotation) {
		Name qualifiedName = getQualifiedName( annotation );
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField" ) ) {
			return Optional.of( new ProcessorGenericFieldProcessor() );
		}
		if ( qualifiedName
				.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.NonStandardField" ) ) {
			return Optional.of( new ProcessorNonStandardFieldProcessor() );
		}
		if ( qualifiedName
				.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField" ) ) {
			return Optional.of( new ProcessorScaledNumberFieldProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField" ) ) {
			return Optional.of( new ProcessorKeywordFieldProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField" ) ) {
			return Optional.of( new ProcessorFulltextFieldProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.VectorField" ) ) {
			return Optional.of( new ProcessorVectorFieldProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding" ) ) {
			return Optional.of( new ProcessorTypePropertyBindingProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding" ) ) {
			return Optional.of( new ProcessorTypePropertyBindingProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded" ) ) {
			return Optional.of( new ProcessorIndexedEmbeddedProcessor() );
		}

		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude" ) ) {
			return Optional.of( new ProcessorLatitudeProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude" ) ) {
			return Optional.of( new ProcessorLongitudeProcessor() );
		}
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding" ) ) {
			return Optional.of( new ProcessorGeoPointBindingProcessor() );
		}

		return Optional.empty();
	}

	private static Name getQualifiedName(AnnotationMirror annotation) {
		return ( (TypeElement) annotation.getAnnotationType().asElement() ).getQualifiedName();
	}

	void process(PropertyMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context);

	void process(PropertyBindingContext bindingContext, AnnotationMirror annotation,
			ProcessorAnnotationProcessorContext context, Element element);
}
