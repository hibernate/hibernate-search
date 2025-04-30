/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public interface ProcessorTypeMappingAnnotationProcessor {
	static Optional<ProcessorTypeMappingAnnotationProcessor> processor(AnnotationMirror annotation) {
		Name qualifiedName = ( (TypeElement) annotation.getAnnotationType().asElement() ).getQualifiedName();
		if ( qualifiedName.contentEquals( "org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding" ) ) {
			return Optional.of( new ProcessorGeoPointBindingProcessor() );
		}

		return Optional.empty();
	}

	void process(TypeMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context);

}
