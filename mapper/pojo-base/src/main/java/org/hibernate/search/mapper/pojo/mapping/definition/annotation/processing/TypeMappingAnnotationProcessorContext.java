/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

/**
 * The context passed to {@link TypeMappingAnnotationProcessor#process(TypeMappingStep, Annotation, TypeMappingAnnotationProcessorContext)}.
 */
public interface TypeMappingAnnotationProcessorContext extends MappingAnnotationProcessorContext {

	@Override
	MappingAnnotatedType annotatedElement();

}
