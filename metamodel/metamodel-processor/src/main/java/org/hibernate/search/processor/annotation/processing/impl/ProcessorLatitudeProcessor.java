/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorLatitudeProcessor extends AbstractProcessorAnnotationProcessor {
	@Override
	public void process(PropertyMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context) {
		mapping.marker( GeoPointBinder.latitude().markerSet( getAnnotationValueAsString( annotation, "markerSet", "" ) ) );
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		context.messager().printMessage( Diagnostic.Kind.ERROR, "Latitude is not allowed within binders.", element );
		return Optional.empty();
	}
}
