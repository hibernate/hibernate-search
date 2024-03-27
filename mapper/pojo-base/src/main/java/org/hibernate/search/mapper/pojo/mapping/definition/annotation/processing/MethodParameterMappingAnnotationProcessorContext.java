/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;

/**
 * The context passed to {@link MethodParameterMappingAnnotationProcessor#process(MethodParameterMappingStep, Annotation, MethodParameterMappingAnnotationProcessorContext)}.
 */
public interface MethodParameterMappingAnnotationProcessorContext extends MappingAnnotationProcessorContext {

	@Override
	MappingAnnotatedMethodParameter annotatedElement();

}
