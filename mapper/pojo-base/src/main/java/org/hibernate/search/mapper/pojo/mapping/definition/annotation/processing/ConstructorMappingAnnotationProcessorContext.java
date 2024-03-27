/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;

/**
 * The context passed to {@link ConstructorMappingAnnotationProcessor#process(ConstructorMappingStep, Annotation, ConstructorMappingAnnotationProcessorContext)}.
 */
public interface ConstructorMappingAnnotationProcessorContext extends MappingAnnotationProcessorContext {

	@Override
	MappingAnnotatedElement annotatedElement();

}
